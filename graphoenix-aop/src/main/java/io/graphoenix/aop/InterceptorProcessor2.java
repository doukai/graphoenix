package io.graphoenix.aop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import io.graphoenix.dagger.ComponentProxyProcessor;
import io.graphoenix.dagger.ProcessorManager;
import io.graphoenix.spi.interceptor.InvocationContextProxy;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.dagger.JavaParserUtil.JAVA_PARSER_UTIL;

@AutoService(ComponentProxyProcessor.class)
public class InterceptorProcessor2 implements ComponentProxyProcessor {

    private ProcessorManager processorManager;
    private List<CompilationUnit> interceptorCompilationUnitList;

    @Override
    public void init(ProcessorManager processorManager) {
        this.processorManager = processorManager;
    }

    @Override
    public void inProcess() {
        this.interceptorCompilationUnitList = processorManager.getCompilationUnitListWithAnnotationClass(Interceptor.class);
    }

    @Override
    public void processComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

        List<String> annotationNameList = interceptorCompilationUnitList.stream()
                .flatMap(compilationUnit -> getAspectAnnotationNameList(compilationUnit).stream())
                .collect(Collectors.toList());

        componentClassDeclaration.getMethods()
                .forEach(methodDeclaration -> {
                            if (methodDeclaration.getAnnotations().stream()
                                    .map(annotationExpr -> processorManager.getNameByType(componentCompilationUnit, annotationExpr.getNameAsString()))
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
                                        .filter(annotationExpr -> annotationNameList.contains(processorManager.getNameByType(componentCompilationUnit, annotationExpr.getNameAsString())))
                                        .collect(Collectors.toList());

                                for (AnnotationExpr annotationExpr : annotationExprList) {
                                    String annotationName = processorManager.getNameByType(componentCompilationUnit, annotationExpr.getNameAsString());
                                    for (Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration> tuple3 : getAroundInvokeMethodList(annotationName)) {

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
                                                                .addArgument(new ThisExpr())
                                                                .setScope(new ObjectCreationExpr().setType(InvocationContextProxy.class))
                                                );

                                        MethodCallExpr proxyMethodCallExpr;
                                        MethodCallExpr addParameterValueExpr;
                                        if (nextContextName == null) {
                                            if (methodDeclaration.getType().isVoidType()) {
                                                proxyMethodCallExpr = new MethodCallExpr()
                                                        .setName("setConsumer")
                                                        .addArgument(
                                                                new MethodReferenceExpr()
                                                                        .setIdentifier(methodDeclaration.getNameAsString().concat("Proxy"))
                                                                        .setScope(new ThisExpr())
                                                        );
                                            } else {
                                                proxyMethodCallExpr = new MethodCallExpr()
                                                        .setName("setFunction")
                                                        .addArgument(
                                                                new MethodReferenceExpr()
                                                                        .setIdentifier(methodDeclaration.getNameAsString().concat("Proxy"))
                                                                        .setScope(new ThisExpr())
                                                        )
                                                        .setScope(createContextExpr);
                                            }

                                            addParameterValueExpr = methodDeclaration.getParameters().stream()
                                                    .map(parameter -> (Expression) parameter.getNameAsExpression())
                                                    .reduce(proxyMethodCallExpr, (left, right) ->
                                                            new MethodCallExpr().setName("addParameterValue")
                                                                    .addArgument(new StringLiteralExpr(right.asNameExpr().getNameAsString()))
                                                                    .addArgument(right)
                                                                    .setScope(left)
                                                    )
                                                    .asMethodCallExpr();
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


                                            addParameterValueExpr = proxyMethodCallExpr;
                                            for (Parameter parameter : methodDeclaration.getParameters()) {
                                                addParameterValueExpr = new MethodCallExpr().setName("addParameterValue")
                                                        .addArgument(new StringLiteralExpr(parameter.getNameAsExpression().asNameExpr().getNameAsString()))
                                                        .addArgument(
                                                                new MethodCallExpr("getParameterValue")
                                                                        .addArgument(new StringLiteralExpr(parameter.getNameAsExpression().asNameExpr().getNameAsString()))
                                                                        .setScope(
                                                                                new EnclosedExpr().setInner(
                                                                                        new CastExpr().setType(InvocationContextProxy.class).setExpression(new NameExpr(nextContextName))
                                                                                )
                                                                        )
                                                        )
                                                        .setScope(addParameterValueExpr);
                                            }
                                        }

                                        Expression addOwnerValueExpr = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .reduce(new MemberValuePair().setValue(addParameterValueExpr), (left, right) ->
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
                                    }
                                }

                                assert nextTuple3 != null;
                                overrideMethodDeclaration.getBody().orElseGet(overrideMethodDeclaration::createBody)
                                        .addStatement(
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

    private List<String> getAspectAnnotationNameList(CompilationUnit compilationUnit) {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();

        List<String> annotationExprList = classOrInterfaceDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> {
                            CompilationUnit annotationCompilationUnit = processorManager.getCompilationUnitByClassOrInterfaceTypeName(compilationUnit, annotationExpr.getNameAsString()).orElseThrow();
                            AnnotationDeclaration annotationDeclaration = JAVA_PARSER_UTIL.getPublicAnnotationDeclaration(annotationCompilationUnit).orElseThrow();
                            return annotationDeclaration.getAnnotations().stream()
                                    .anyMatch(subAnnotationExpr -> processorManager.getNameByType(annotationCompilationUnit, subAnnotationExpr.getNameAsString()).equals(InterceptorBinding.class.getName()));
                        }
                )
                .map(annotationExpr -> processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()))
                .collect(Collectors.toList());


        List<String> subAnnotationExprList = classOrInterfaceDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> {
                            CompilationUnit annotationCompilationUnit = processorManager.getCompilationUnitByClassOrInterfaceTypeName(compilationUnit, annotationExpr.getNameAsString()).orElseThrow();
                            AnnotationDeclaration annotationDeclaration = JAVA_PARSER_UTIL.getPublicAnnotationDeclaration(annotationCompilationUnit).orElseThrow();
                            return annotationDeclaration.getAnnotations().stream()
                                    .anyMatch(subAnnotationExpr -> annotationExprList.contains(processorManager.getNameByType(annotationCompilationUnit, subAnnotationExpr.getNameAsString())));
                        }
                )
                .map(annotationExpr -> processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()))
                .collect(Collectors.toList());

        return Stream.concat(annotationExprList.stream(), subAnnotationExprList.stream()).collect(Collectors.toList());
    }

    private List<Tuple3<CompilationUnit, ClassOrInterfaceDeclaration, MethodDeclaration>> getAroundInvokeMethodList(String annotationName) {

        return interceptorCompilationUnitList.stream()
                .flatMap(compilationUnit -> {
                            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();
                            return classOrInterfaceDeclaration.getMethods().stream()
                                    .map(methodDeclaration -> Tuple.of(compilationUnit, classOrInterfaceDeclaration, methodDeclaration));
                        }
                )
                .filter(tuple3 ->
                        tuple3._2().getAnnotations().stream().map(annotationExpr -> processorManager.getNameByType(tuple3._1(), annotationExpr.getNameAsString()))
                                .anyMatch(name -> name.equals(annotationName))
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
