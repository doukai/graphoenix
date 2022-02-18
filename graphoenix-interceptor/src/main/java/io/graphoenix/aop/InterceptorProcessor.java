package io.graphoenix.aop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import io.graphoenix.inject.ComponentProxyProcessor;
import io.graphoenix.inject.ProcessorManager;
import io.graphoenix.spi.interceptor.InvocationContextProxy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.tinylog.Logger;

import javax.tools.FileObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(ComponentProxyProcessor.class)
public class InterceptorProcessor implements ComponentProxyProcessor {

    private ProcessorManager processorManager;
    private List<CompilationUnit> interceptorCompilationUnitList;

    @Override
    public void init(ProcessorManager processorManager) {
        this.processorManager = processorManager;
    }

    @Override
    public void inProcess() {
        this.interceptorCompilationUnitList = processorManager.getCompilationUnitListWithAnnotationClass(Interceptor.class);
        this.interceptorCompilationUnitList.stream()
                .flatMap(compilationUnit -> {
                            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);
                            return getAspectAnnotationNameList(compilationUnit).stream().map(annotationName -> Tuple.of(annotationName, classOrInterfaceDeclaration.getFullyQualifiedName().orElse(classOrInterfaceDeclaration.getNameAsString())));
                        }
                )
                .collect(Collectors.groupingBy(Tuple2::_1, Collectors.mapping(Tuple2::_2, Collectors.toSet())))
                .forEach((key, value) -> {
                            Optional<FileObject> fileObject = processorManager.getResource("META-INF/interceptor/".concat(key));
                            if (fileObject.isPresent()) {
                                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileObject.get().openInputStream()))) {
                                    String line;
                                    while ((line = bufferedReader.readLine()) != null) {
                                        Logger.info("add interceptor {} to annotation {}", line, key);
                                        value.add(line);
                                    }
                                } catch (IOException e) {
                                    Logger.warn(e);
                                }
                            }
                            processorManager.createResource("META-INF/interceptor/".concat(key), String.join(System.lineSeparator(), value));
                            Logger.info("annotation interceptor resource build success: {}", key);
                        }
                );
    }

    @Override
    public void processComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

        List<String> annotationNameList = interceptorCompilationUnitList.stream()
                .flatMap(compilationUnit -> getAspectAnnotationNameList(compilationUnit).stream())
                .collect(Collectors.toList());

        buildMethod(annotationNameList, componentClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration);
        buildConstructor(annotationNameList, componentClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration);
    }

    private void buildMethod(List<String> annotationNameList, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        componentClassDeclaration.getMethods()
                .forEach(methodDeclaration -> {
                            if (methodDeclaration.getAnnotations().stream()
                                    .map(annotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(annotationExpr))
                                    .anyMatch(annotationNameList::contains)
                            ) {
                                componentProxyCompilationUnit.addImport(InvocationContext.class);
                                componentProxyCompilationUnit.addImport(InvocationContextProxy.class);
                                componentProxyCompilationUnit.addImport("io.graphoenix.core.context.BeanContext");

                                MethodDeclaration overrideMethodDeclaration = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString())
                                        .setModifiers(methodDeclaration.getModifiers())
                                        .setParameters(methodDeclaration.getParameters())
                                        .setType(methodDeclaration.getType())
                                        .addAnnotation(Override.class);

                                MethodDeclaration proxyMethodDeclaration = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString().concat("Proxy"))
                                        .setModifiers(methodDeclaration.getModifiers())
                                        .addParameter(InvocationContext.class, "invocationContext")
                                        .addThrownException(Exception.class);

                                VariableDeclarationExpr invocationContextProxyVariable = new VariableDeclarationExpr()
                                        .addVariable(new VariableDeclarator()
                                                .setType(InvocationContextProxy.class)
                                                .setName("invocationContextProxy")
                                                .setInitializer(new CastExpr()
                                                        .setType(InvocationContextProxy.class)
                                                        .setExpression(new NameExpr("invocationContext"))
                                                )
                                        );

                                MethodCallExpr superMethodCallExpr = new MethodCallExpr()
                                        .setName(methodDeclaration.getName())
                                        .setArguments(
                                                methodDeclaration.getParameters().stream()
                                                        .map(parameter ->
                                                                new CastExpr()
                                                                        .setType(parameter.getType())
                                                                        .setExpression(
                                                                                new MethodCallExpr()
                                                                                        .setName("getParameterValue")
                                                                                        .setScope(new NameExpr("invocationContextProxy"))
                                                                                        .addArgument(new StringLiteralExpr(parameter.getNameAsString()))
                                                                        )
                                                        )
                                                        .collect(Collectors.toCollection(NodeList::new))
                                        )
                                        .setScope(new SuperExpr());

                                if (methodDeclaration.getType().isVoidType()) {
                                    proxyMethodDeclaration.setType(methodDeclaration.getType());
                                    proxyMethodDeclaration.createBody().addStatement(invocationContextProxyVariable).addStatement(superMethodCallExpr);
                                } else {
                                    proxyMethodDeclaration.setType(Object.class);
                                    proxyMethodDeclaration.createBody().addStatement(invocationContextProxyVariable).addStatement(new ReturnStmt(superMethodCallExpr));
                                }

                                String nextContextName = null;
                                Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration> nextTuple3 = null;

                                List<AnnotationExpr> annotationExprList = methodDeclaration.getAnnotations().stream()
                                        .filter(annotationExpr -> annotationNameList.contains(processorManager.getQualifiedNameByAnnotationExpr(annotationExpr)))
                                        .collect(Collectors.toList());

                                for (AnnotationExpr annotationExpr : annotationExprList) {
                                    String annotationName = processorManager.getQualifiedNameByAnnotationExpr(annotationExpr);
                                    for (Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration> tuple3 : getInterceptorMethodList(annotationName, AroundInvoke.class)) {

                                        componentProxyCompilationUnit.addImport(annotationName);
                                        CompilationUnit invokeCompilationUnit = tuple3._1();
                                        ClassOrInterfaceDeclaration invokeClassOrInterfaceDeclaration = tuple3._2();
                                        MethodDeclaration invokeMethodDeclaration = tuple3._3();

                                        String interceptorFieldName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
                                                annotationExpr.getNameAsString().concat("_").concat(invokeClassOrInterfaceDeclaration.getNameAsString()).concat("_").concat(invokeMethodDeclaration.getNameAsString()));
                                        String contextName = interceptorFieldName.concat("Context");

                                        Expression createContextExpr = new MethodCallExpr()
                                                .setName("setOwner")
                                                .addArgument(new ClassExpr().setType(annotationExpr.getName().getIdentifier()))
                                                .setScope(
                                                        new MethodCallExpr()
                                                                .setName("setTarget")
                                                                .addArgument(methodDeclaration.isStatic() ? new ClassExpr().setType(componentProxyClassDeclaration.getNameAsString()) : new ThisExpr())
                                                                .setScope(new ObjectCreationExpr().setType(InvocationContextProxy.class))
                                                );

                                        MethodCallExpr proxyMethodCallExpr;
                                        if (nextContextName == null) {
                                            if (methodDeclaration.getType().isVoidType()) {
                                                proxyMethodCallExpr = new MethodCallExpr()
                                                        .setName("setConsumer")
                                                        .addArgument(
                                                                new MethodReferenceExpr()
                                                                        .setIdentifier(methodDeclaration.getNameAsString().concat("Proxy"))
                                                                        .setScope(methodDeclaration.isStatic() ? new NameExpr(componentProxyClassDeclaration.getNameAsString()) : new ThisExpr())
                                                        );
                                            } else {
                                                proxyMethodCallExpr = new MethodCallExpr()
                                                        .setName("setFunction")
                                                        .addArgument(
                                                                new MethodReferenceExpr()
                                                                        .setIdentifier(methodDeclaration.getNameAsString().concat("Proxy"))
                                                                        .setScope(methodDeclaration.isStatic() ? new NameExpr(componentProxyClassDeclaration.getNameAsString()) : new ThisExpr())
                                                        )
                                                        .setScope(createContextExpr);
                                            }
                                        } else {
                                            proxyMethodCallExpr = new MethodCallExpr()
                                                    .setName("setNextInvocationContext")
                                                    .addArgument(new NameExpr(nextContextName))
                                                    .setScope(
                                                            new MethodCallExpr()
                                                                    .setName("setNextProceed")
                                                                    .addArgument(
                                                                            new MethodReferenceExpr()
                                                                                    .setIdentifier(nextTuple3._3().getNameAsString())
                                                                                    .setScope(
                                                                                            new MethodCallExpr("get")
                                                                                                    .setScope(
                                                                                                            new MethodCallExpr("getProvider")
                                                                                                                    .setScope(new NameExpr("BeanContext"))
                                                                                                                    .addArgument(new ClassExpr().setType(nextTuple3._2().getNameAsString()))
                                                                                                    )
                                                                                    )
                                                                    )
                                                                    .setScope(createContextExpr)
                                                    );
                                        }

                                        Expression addOwnerValueExpr = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .reduce(new MemberValuePair().setValue(proxyMethodCallExpr), (left, right) ->
                                                        new MemberValuePair().setValue(
                                                                new MethodCallExpr().setName("addOwnerValue")
                                                                        .addArgument(new StringLiteralExpr(right.getNameAsString()))
                                                                        .addArgument(right.getValue())
                                                                        .setScope(left.getValue()))
                                                )
                                                .getValue();

                                        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr()
                                                .addVariable(new VariableDeclarator()
                                                        .setType(InvocationContext.class)
                                                        .setName(contextName)
                                                        .setInitializer(addOwnerValueExpr)
                                                );

                                        overrideMethodDeclaration.getBody().orElseGet(overrideMethodDeclaration::createBody).addStatement(variableDeclarationExpr);

                                        nextContextName = contextName;
                                        nextTuple3 = tuple3;

                                        Logger.info("{}.{} add interceptor {}.{} for annotation {}",
                                                processorManager.getQualifiedNameByDeclaration(componentClassDeclaration),
                                                methodDeclaration.getNameAsString(),
                                                processorManager.getQualifiedNameByDeclaration(invokeClassOrInterfaceDeclaration),
                                                invokeMethodDeclaration.getNameAsString(),
                                                annotationName
                                        );
                                    }
                                }

                                assert nextTuple3 != null;
                                BlockStmt blockStmt = overrideMethodDeclaration.getBody().orElseGet(overrideMethodDeclaration::createBody);
                                blockStmt.getStatements().getLast()
                                        .ifPresent(statement -> {
                                                    StringLiteralExpr methodName = new StringLiteralExpr(methodDeclaration.getNameAsString());
                                                    IntegerLiteralExpr methodParameterCount = new IntegerLiteralExpr(String.valueOf(methodDeclaration.getParameters().size()));
                                                    ArrayCreationExpr methodParameterTypeNames = new ArrayCreationExpr().setElementType(String[].class)
                                                            .setInitializer(
                                                                    new ArrayInitializerExpr(
                                                                            methodDeclaration.getParameters().stream().map(parameter -> {
                                                                                                if (parameter.getType().isClassOrInterfaceType()) {
                                                                                                    return processorManager.getQualifiedNameByType(parameter.getType().asClassOrInterfaceType());
                                                                                                } else {
                                                                                                    return parameter.getType().asString();
                                                                                                }
                                                                                            }
                                                                                    )
                                                                                    .map(StringLiteralExpr::new)
                                                                                    .collect(Collectors.toCollection(NodeList::new))
                                                                    )
                                                            );

                                                    blockStmt.replace(
                                                            statement,
                                                            new ExpressionStmt(
                                                                    new MethodCallExpr()
                                                                            .setName("setMethod")
                                                                            .addArgument(methodName)
                                                                            .addArgument(methodParameterCount)
                                                                            .addArgument(methodParameterTypeNames)
                                                                            .setScope(
                                                                                    methodDeclaration.getParameters().stream()
                                                                                            .map(parameter -> (Expression) parameter.getNameAsExpression())
                                                                                            .reduce(statement.asExpressionStmt().getExpression(), (left, right) ->
                                                                                                    new MethodCallExpr().setName("addParameterValue")
                                                                                                            .addArgument(new StringLiteralExpr(right.asNameExpr().getNameAsString()))
                                                                                                            .addArgument(right)
                                                                                                            .setScope(left)
                                                                                            )
                                                                                            .asMethodCallExpr()
                                                                            )


                                                            )
                                                    );
                                                }
                                        );

                                blockStmt.addStatement(
                                        new ReturnStmt(
                                                new CastExpr()
                                                        .setType(methodDeclaration.getType())
                                                        .setExpression(
                                                                new MethodCallExpr()
                                                                        .setName(nextTuple3._3().getNameAsString())
                                                                        .addArgument(new NameExpr(nextContextName))
                                                                        .setScope(
                                                                                new MethodCallExpr("get")
                                                                                        .setScope(
                                                                                                new MethodCallExpr("getProvider")
                                                                                                        .setScope(new NameExpr("BeanContext"))
                                                                                                        .addArgument(new ClassExpr().setType(nextTuple3._2().getNameAsString()))
                                                                                        )
                                                                        )
                                                        )
                                        )
                                );
                            }
                        }
                );
    }

    private void buildConstructor(List<String> annotationNameList, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        componentClassDeclaration.getConstructors()
                .forEach(constructorDeclaration -> {
                            if (constructorDeclaration.getAnnotations().stream()
                                    .map(annotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(annotationExpr))
                                    .anyMatch(annotationNameList::contains)
                            ) {
                                componentProxyCompilationUnit.addImport(InvocationContext.class);
                                componentProxyCompilationUnit.addImport(InvocationContextProxy.class);
                                componentProxyCompilationUnit.addImport("io.graphoenix.core.context.BeanContext");

                                MethodDeclaration creatorMethod = componentProxyClassDeclaration.addMethod("create".concat(componentProxyClassDeclaration.getNameAsString()))
                                        .setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                        .setParameters(constructorDeclaration.getParameters())
                                        .addAnnotation(Produces.class)
                                        .setThrownExceptions(constructorDeclaration.getThrownExceptions());

                                MethodDeclaration invocationCreatorMethod = componentProxyClassDeclaration.addMethod("create".concat(componentProxyClassDeclaration.getNameAsString()))
                                        .setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                                        .setType(Object.class)
                                        .addParameter(InvocationContext.class, "invocationContext")
                                        .addThrownException(Exception.class);

                                VariableDeclarationExpr invocationContextProxyVariable = new VariableDeclarationExpr()
                                        .addVariable(new VariableDeclarator()
                                                .setType(InvocationContextProxy.class)
                                                .setName("invocationContextProxy")
                                                .setInitializer(new CastExpr()
                                                        .setType(InvocationContextProxy.class)
                                                        .setExpression(new NameExpr("invocationContext"))
                                                )
                                        );

                                ObjectCreationExpr invocationCreatorMethodCallExpr = new ObjectCreationExpr()
                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                        .setArguments(
                                                constructorDeclaration.getParameters().stream()
                                                        .map(parameter ->
                                                                new CastExpr()
                                                                        .setType(parameter.getType())
                                                                        .setExpression(
                                                                                new MethodCallExpr()
                                                                                        .setName("getParameterValue")
                                                                                        .setScope(new NameExpr("invocationContextProxy"))
                                                                                        .addArgument(new StringLiteralExpr(parameter.getNameAsString()))
                                                                        )
                                                        )
                                                        .collect(Collectors.toCollection(NodeList::new))
                                        );

                                invocationCreatorMethod.createBody().addStatement(invocationContextProxyVariable).addStatement(new ReturnStmt(invocationCreatorMethodCallExpr));

                                String nextContextName = null;
                                Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration> nextTuple3 = null;

                                List<AnnotationExpr> annotationExprList = constructorDeclaration.getAnnotations().stream()
                                        .filter(annotationExpr -> annotationNameList.contains(processorManager.getQualifiedNameByAnnotationExpr(annotationExpr)))
                                        .collect(Collectors.toList());

                                for (AnnotationExpr annotationExpr : annotationExprList) {
                                    String annotationName = processorManager.getQualifiedNameByAnnotationExpr(annotationExpr);
                                    for (Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration> tuple3 : getInterceptorMethodList(annotationName, AroundConstruct.class)) {

                                        componentProxyCompilationUnit.addImport(annotationName);
                                        CompilationUnit invokeCompilationUnit = tuple3._1();
                                        ClassOrInterfaceDeclaration invokeClassOrInterfaceDeclaration = tuple3._2();
                                        MethodDeclaration invokeMethodDeclaration = tuple3._3();

                                        String interceptorFieldName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
                                                annotationExpr.getNameAsString().concat("_").concat(invokeClassOrInterfaceDeclaration.getNameAsString()).concat("_").concat(invokeMethodDeclaration.getNameAsString()));
                                        String contextName = interceptorFieldName.concat("Context");

                                        Expression createContextExpr = new MethodCallExpr()
                                                .setName("setOwner")
                                                .addArgument(new ClassExpr().setType(annotationExpr.getName().getIdentifier()))
                                                .setScope(
                                                        new MethodCallExpr()
                                                                .setName("setTarget")
                                                                .addArgument(new ClassExpr().setType(componentProxyClassDeclaration.getNameAsString()))
                                                                .setScope(new ObjectCreationExpr().setType(InvocationContextProxy.class))
                                                );

                                        MethodCallExpr proxyMethodCallExpr;
                                        if (nextContextName == null) {
                                            proxyMethodCallExpr = new MethodCallExpr()
                                                    .setName("setFunction")
                                                    .addArgument(
                                                            new MethodReferenceExpr()
                                                                    .setIdentifier("create".concat(componentProxyClassDeclaration.getNameAsString()))
                                                                    .setScope(new NameExpr(componentProxyClassDeclaration.getNameAsString()))
                                                    )
                                                    .setScope(createContextExpr);
                                        } else {
                                            proxyMethodCallExpr = new MethodCallExpr()
                                                    .setName("setNextInvocationContext")
                                                    .addArgument(new NameExpr(nextContextName))
                                                    .setScope(
                                                            new MethodCallExpr()
                                                                    .setName("setNextProceed")
                                                                    .addArgument(
                                                                            new MethodReferenceExpr()
                                                                                    .setIdentifier(nextTuple3._3().getNameAsString())
                                                                                    .setScope(
                                                                                            new MethodCallExpr("get")
                                                                                                    .setScope(
                                                                                                            new MethodCallExpr("getProvider")
                                                                                                                    .setScope(new NameExpr("BeanContext"))
                                                                                                                    .addArgument(new ClassExpr().setType(nextTuple3._2().getNameAsString()))
                                                                                                    )
                                                                                    )
                                                                    )
                                                                    .setScope(createContextExpr)
                                                    );
                                        }

                                        Expression addOwnerValueExpr = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .reduce(new MemberValuePair().setValue(proxyMethodCallExpr), (left, right) ->
                                                        new MemberValuePair().setValue(
                                                                new MethodCallExpr().setName("addOwnerValue")
                                                                        .addArgument(new StringLiteralExpr(right.getNameAsString()))
                                                                        .addArgument(right.getValue())
                                                                        .setScope(left.getValue()))
                                                )
                                                .getValue();

                                        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr()
                                                .addVariable(new VariableDeclarator()
                                                        .setType(InvocationContext.class)
                                                        .setName(contextName)
                                                        .setInitializer(addOwnerValueExpr)
                                                );

                                        creatorMethod.getBody().orElseGet(creatorMethod::createBody).addStatement(variableDeclarationExpr);

                                        nextContextName = contextName;
                                        nextTuple3 = tuple3;

                                        Logger.info("{}.{} add interceptor {}.{} for annotation {}",
                                                processorManager.getQualifiedNameByDeclaration(componentClassDeclaration),
                                                constructorDeclaration.getNameAsString(),
                                                processorManager.getQualifiedNameByDeclaration(invokeClassOrInterfaceDeclaration),
                                                invokeMethodDeclaration.getNameAsString(),
                                                annotationName
                                        );
                                    }
                                }

                                assert nextTuple3 != null;
                                BlockStmt blockStmt = creatorMethod.getBody().orElseGet(invocationCreatorMethod::createBody);
                                blockStmt.getStatements().getLast()
                                        .ifPresent(statement -> {
                                                    IntegerLiteralExpr constructorParameterCount = new IntegerLiteralExpr(String.valueOf(constructorDeclaration.getParameters().size()));
                                                    ArrayCreationExpr constructorParameterTypeNames = new ArrayCreationExpr().setElementType(String[].class)
                                                            .setInitializer(
                                                                    new ArrayInitializerExpr(
                                                                            constructorDeclaration.getParameters().stream().map(parameter -> {
                                                                                                if (parameter.getType().isClassOrInterfaceType()) {
                                                                                                    return processorManager.getQualifiedNameByType(parameter.getType().asClassOrInterfaceType());
                                                                                                } else {
                                                                                                    return parameter.getType().asString();
                                                                                                }
                                                                                            }
                                                                                    )
                                                                                    .map(StringLiteralExpr::new)
                                                                                    .collect(Collectors.toCollection(NodeList::new))
                                                                    )
                                                            );

                                                    blockStmt.replace(
                                                            statement,
                                                            new ExpressionStmt(
                                                                    new MethodCallExpr()
                                                                            .setName("setConstructor")
                                                                            .addArgument(constructorParameterCount)
                                                                            .addArgument(constructorParameterTypeNames)
                                                                            .setScope(
                                                                                    constructorDeclaration.getParameters().stream()
                                                                                            .map(parameter -> (Expression) parameter.getNameAsExpression())
                                                                                            .reduce(statement.asExpressionStmt().getExpression(), (left, right) ->
                                                                                                    new MethodCallExpr().setName("addParameterValue")
                                                                                                            .addArgument(new StringLiteralExpr(right.asNameExpr().getNameAsString()))
                                                                                                            .addArgument(right)
                                                                                                            .setScope(left)
                                                                                            )
                                                                                            .asMethodCallExpr()
                                                                            )


                                                            )
                                                    );
                                                }
                                        );

                                blockStmt.addStatement(
                                        new ReturnStmt(
                                                new CastExpr()
                                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                                        .setExpression(
                                                                new MethodCallExpr()
                                                                        .setName(nextTuple3._3().getNameAsString())
                                                                        .addArgument(new NameExpr(nextContextName))
                                                                        .setScope(
                                                                                new MethodCallExpr("get")
                                                                                        .setScope(
                                                                                                new MethodCallExpr("getProvider")
                                                                                                        .setScope(new NameExpr("BeanContext"))
                                                                                                        .addArgument(new ClassExpr().setType(nextTuple3._2().getNameAsString()))
                                                                                        )
                                                                        )
                                                        )
                                        )
                                );
                            }
                        }
                );
    }


    private List<String> getAspectAnnotationNameList(CompilationUnit compilationUnit) {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);

        List<String> annotationExprList = classOrInterfaceDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> {
                            CompilationUnit annotationCompilationUnit = processorManager.getCompilationUnitByAnnotationExpr(annotationExpr);
                            AnnotationDeclaration annotationDeclaration = processorManager.getPublicAnnotationDeclaration(annotationCompilationUnit);
                            return annotationDeclaration.getAnnotations().stream()
                                    .anyMatch(subAnnotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(subAnnotationExpr).equals(InterceptorBinding.class.getName()));
                        }
                )
                .map(annotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(annotationExpr))
                .collect(Collectors.toList());

        List<String> subAnnotationExprList = classOrInterfaceDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> {
                            CompilationUnit annotationCompilationUnit = processorManager.getCompilationUnitByAnnotationExpr(annotationExpr);
                            AnnotationDeclaration annotationDeclaration = processorManager.getPublicAnnotationDeclaration(annotationCompilationUnit);
                            return annotationDeclaration.getAnnotations().stream()
                                    .anyMatch(subAnnotationExpr -> annotationExprList.contains(processorManager.getQualifiedNameByAnnotationExpr(subAnnotationExpr)));
                        }
                )
                .map(annotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(annotationExpr))
                .collect(Collectors.toList());

        return Stream.concat(annotationExprList.stream(), subAnnotationExprList.stream()).collect(Collectors.toList());
    }

    private List<Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration>> getInterceptorMethodList(String annotationName, Class<? extends Annotation> annotationClass) {

        Set<String> interceptorClassName = new HashSet<>();
        try {
            Iterator<URL> urlIterator = Objects.requireNonNull(InterceptorProcessor.class.getClassLoader().getResources("META-INF/interceptor/".concat(annotationName))).asIterator();
            while (urlIterator.hasNext()) {
                URL url = urlIterator.next();
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        interceptorClassName.add(line);
                        Logger.info("find interceptor class {} for {}", line, annotationName);
                    }
                }
            }
        } catch (IOException e) {
            Logger.warn(e);
        }

        return Streams.concat(
                        interceptorClassName.stream().map(className -> processorManager.getCompilationUnitByQualifiedName(className)),
                        interceptorCompilationUnitList.stream()
                )
                .flatMap(compilationUnit -> {
                            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);
                            return classOrInterfaceDeclaration.getMethods().stream().map(methodDeclaration -> Tuple.of(compilationUnit, classOrInterfaceDeclaration, methodDeclaration));
                        }
                )
                .filter(tuple3 ->
                        tuple3._2().getAnnotations().stream().map(annotationExpr -> processorManager.getQualifiedNameByAnnotationExpr(annotationExpr))
                                .anyMatch(name -> name.equals(annotationName)) &&
                                tuple3._3().isAnnotationPresent(annotationClass)
                )
                .sorted(Comparator.comparing(tuple3 -> getPriorityFromInterceptor(tuple3._2()), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private int getPriorityFromInterceptor(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getAnnotationByClass(Priority.class)
                .flatMap(annotationExpr ->
                        annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                .map(memberValuePair -> memberValuePair.getValue().asIntegerLiteralExpr().asNumber().intValue())
                                .findFirst()
                )
                .orElse(Integer.MAX_VALUE);
    }
}
