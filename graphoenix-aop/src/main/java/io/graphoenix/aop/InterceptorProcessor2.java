package io.graphoenix.aop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import io.graphoenix.dagger.ComponentProxyProcessor;
import io.graphoenix.dagger.ProcessorManager;
import io.graphoenix.spi.aop.InvocationContext;
import io.graphoenix.spi.interceptor.InvocationContextProxy;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.interceptor.Interceptor;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
                                componentProxyCompilationUnit.addImport(InvocationContextProxy.class);

                                MethodDeclaration overrideMethodDeclaration = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString())
                                        .setModifiers(methodDeclaration.getModifiers())
                                        .setParameters(methodDeclaration.getParameters())
                                        .setType(methodDeclaration.getType())
                                        .addAnnotation(Override.class);

                                MethodDeclaration proxyMethodDeclaration = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString().concat("Proxy"))
                                        .setModifiers(methodDeclaration.getModifiers())
                                        .addParameter(InvocationContextProxy.class, "invocationContextProxy")
                                        .addAnnotation(Override.class);

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
                                    proxyMethodDeclaration.createBody().addStatement(superMethodCallExpr);
                                } else {
                                    proxyMethodDeclaration.setType(Object.class);
                                    proxyMethodDeclaration.createBody().addStatement(new ReturnStmt(superMethodCallExpr));
                                }

                                methodDeclaration.getAnnotations().stream()
                                        .filter(annotationExpr -> annotationNameList.contains(processorManager.getNameByType(componentCompilationUnit, annotationExpr.getNameAsString())))
                                        .forEachOrdered(annotationExpr -> {
                                                    getAroundInvokeMethodList(processorManager.getNameByType(componentCompilationUnit, annotationExpr.getNameAsString()))
                                                            .stream().sorted(Collections.reverseOrder())
                                                            .collect(Collectors.toList())
                                                            .forEach(tuple3 -> {
                                                                        CompilationUnit invokeCompilationUnit = tuple3._1();
                                                                        ClassOrInterfaceDeclaration invokeClassOrInterfaceDeclaration = tuple3._2();
                                                                        MethodDeclaration invokeMethodDeclaration = tuple3._3();

                                                                        String interceptorFieldName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, annotationExpr.getNameAsString().concat("_").concat(invokeClassOrInterfaceDeclaration.getNameAsString()));

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
                                                                                    );
                                                                        }
                                                                        proxyMethodCallExpr.setScope(createContextExpr);

                                                                        Expression addParameterValueExpr = methodDeclaration.getParameters().stream()
                                                                                .map(parameter -> (Expression) parameter.getNameAsExpression())
                                                                                .reduce(proxyMethodCallExpr, (left, right) ->
                                                                                        new MethodCallExpr().setName("addParameterValue")
                                                                                                .addArgument(new StringLiteralExpr(right.asNameExpr().getNameAsString()))
                                                                                                .addArgument(right)
                                                                                                .setScope(left)
                                                                                );

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
                                                                                        .setName(interceptorFieldName.concat("Context"))
                                                                                        .setInitializer(addOwnerValueExpr)
                                                                                );
                                                                    }
                                                            );
                                                }
                                        );


                            }
                        }
                );
    }

    private List<String> getAspectAnnotationNameList(CompilationUnit compilationUnit) {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();

        return classOrInterfaceDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> !processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()).equals(Interceptor.class.getName()))
                .filter(annotationExpr -> !processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()).equals(Priority.class.getName()))
                .filter(annotationExpr -> !processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()).equals(Singleton.class.getName()))
                .filter(annotationExpr -> !processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()).equals(Dependent.class.getName()))
                .filter(annotationExpr -> !processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()).equals(ApplicationScoped.class.getName()))
                .map(annotationExpr -> processorManager.getNameByType(compilationUnit, annotationExpr.getNameAsString()))
                .collect(Collectors.toList());
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
                .sorted(Comparator.comparing(tuple3 -> getPriorityFromInterceptor(tuple3._2())))
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
