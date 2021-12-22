package io.graphoenix.aop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.aop.InvocationContext;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;

@AutoService(DaggerProxyProcessor.class)
public class InterceptorProcessor implements DaggerProxyProcessor {

    @Override
    public void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                                    ClassOrInterfaceDeclaration moduleClassDeclaration,
                                    CompilationUnit componentCompilationUnit,
                                    ClassOrInterfaceDeclaration componentClassDeclaration,
                                    CompilationUnit componentProxyCompilationUnit,
                                    ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

        List<MethodDeclaration> interceptorBeanMethodDeclarations = componentClassDeclaration.getMembers().stream()
                .filter(bodyDeclaration -> !bodyDeclaration.isConstructorDeclaration())
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                .flatMap(moduleMethodDeclaration -> moduleMethodDeclaration.getAnnotations().stream()
                        .map(annotationExpr -> getInterceptorBeanMethodDeclaration(moduleClassDeclaration, annotationExpr))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                )
                .collect(Collectors.toList());

        interceptorBeanMethodDeclarations.forEach(interceptorBeanMethodDeclaration -> componentProxyClassDeclaration.addField(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE));

        componentClassDeclaration.getConstructors().forEach(
                constructorDeclaration -> componentProxyClassDeclaration.getConstructors()
                        .forEach(componentProxyClassConstructor -> {
                                    interceptorBeanMethodDeclarations
                                            .forEach(interceptorBeanMethodDeclaration -> {
                                                        componentProxyClassConstructor.addParameter(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString());
                                                        componentProxyClassConstructor.getBody().addStatement(
                                                                new AssignExpr()
                                                                        .setTarget(new FieldAccessExpr().setName(interceptorBeanMethodDeclaration.getName()).setScope(new ThisExpr()))
                                                                        .setOperator(ASSIGN)
                                                                        .setValue(interceptorBeanMethodDeclaration.getNameAsExpression())
                                                        );
                                                    }
                                            );
                                }
                        )
        );

        componentClassDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .forEach(componentMethodDeclaration -> {

                    Map<AnnotationExpr, MethodDeclaration> interceptorBeanMethodMap = componentMethodDeclaration.getAnnotations().stream()
                            .collect(Collectors.toMap(annotationExpr -> annotationExpr, annotationExpr -> getInterceptorBeanMethodDeclaration(moduleClassDeclaration, annotationExpr)))
                            .entrySet().stream()
                            .filter(entry -> entry.getValue().isPresent())
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

                    if (interceptorBeanMethodMap.keySet().size() > 0) {
                        BlockStmt blockStmt = componentProxyClassDeclaration.addMethod(componentMethodDeclaration.getNameAsString(), componentMethodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                .setParameters(componentMethodDeclaration.getParameters())
                                .setType(componentMethodDeclaration.getType())
                                .addAnnotation(Override.class)
                                .createBody();

                        interceptorBeanMethodMap.forEach((key, value) -> {
                            Expression createContextExpr = new MethodCallExpr()
                                    .setName("setOwner")
                                    .addArgument(new ClassExpr().setType(key.getName().getIdentifier()))
                                    .setScope(
                                            new MethodCallExpr()
                                                    .setName("setTarget")
                                                    .addArgument(new ThisExpr())
                                                    .setScope(
                                                            new MethodCallExpr()
                                                                    .setName("setName")
                                                                    .addArgument(new StringLiteralExpr(componentMethodDeclaration.getNameAsString()))
                                                                    .setScope(
                                                                            new ObjectCreationExpr()
                                                                                    .setType(InvocationContext.class)
                                                                    )
                                                    )
                                    );

                            Expression addParameterValueExpr = componentMethodDeclaration.getParameters().stream()
                                    .map(parameter -> (Expression) parameter.getNameAsExpression())
                                    .reduce(createContextExpr, (pre, current) ->
                                            new MethodCallExpr().setName("addParameterValue")
                                                    .addArgument(new StringLiteralExpr(current.asNameExpr().getNameAsString()))
                                                    .addArgument(current)
                                                    .setScope(pre)
                                    );

                            Expression addOwnerValueExpr = key.asNormalAnnotationExpr().getPairs().stream()
                                    .reduce(new MemberValuePair().setValue(addParameterValueExpr), (pre, current) ->
                                            new MemberValuePair().setValue(
                                                    new MethodCallExpr().setName("addOwnerValue")
                                                            .addArgument(new StringLiteralExpr(current.getNameAsString()))
                                                            .addArgument(current.getValue())
                                                            .setScope(pre.getValue()))
                                    )
                                    .getValue();

                            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr()
                                    .addVariable(new VariableDeclarator()
                                            .setType(InvocationContext.class)
                                            .setName(value.getName().getIdentifier().concat("Context"))
                                            .setInitializer(addOwnerValueExpr)
                                    );

                            blockStmt.addStatement(variableDeclarationExpr);
                            blockStmt.addStatement(new MethodCallExpr().setName("before").addArgument(value.getName().getIdentifier().concat("Context")).setScope(value.getNameAsExpression()));
                        });

                        if (!componentMethodDeclaration.getType().isVoidType()) {
                            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr().addVariable(
                                    new VariableDeclarator()
                                            .setName("result")
                                            .setType(componentMethodDeclaration.getType())
                                            .setInitializer(
                                                    new MethodCallExpr()
                                                            .setName(componentMethodDeclaration.getName())
                                                            .setArguments(new NodeList<>(componentMethodDeclaration.getParameters().stream().map(NodeWithSimpleName::getNameAsExpression).collect(Collectors.toList())))
                                                            .setScope(new SuperExpr())
                                            ));

                            blockStmt.addStatement(variableDeclarationExpr);
                        }

                        interceptorBeanMethodMap.forEach((key, value) -> {
                            if (!componentMethodDeclaration.getType().isVoidType()) {
                                blockStmt.addStatement(new MethodCallExpr().setName("setReturnValue").addArgument("result").setScope(new NameExpr(value.getName().getIdentifier().concat("Context"))));
                            }

                            blockStmt.addStatement(new MethodCallExpr().setName("after").addArgument(value.getName().getIdentifier().concat("Context")).setScope(value.getNameAsExpression()));

                            if (!componentMethodDeclaration.getType().isVoidType()) {
                                blockStmt.addStatement(new ReturnStmt().setExpression(new NameExpr("result")));
                            }
                        });
                    }
                });

        componentProxyCompilationUnit.addImport(InvocationContext.class);
    }

    @Override
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleCLassDeclaration,
                                 List<CompilationUnit> componentProxyCompilationUnits,
                                 CompilationUnit moduleProxyCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {
        List<MethodDeclaration> interceptorBeanMethodDeclarations = getInterceptorBeanMethodDeclarations(moduleCLassDeclaration);
        interceptorBeanMethodDeclarations
                .forEach(methodDeclaration -> {
                            moduleProxyClassDeclaration
                                    .addMethod(methodDeclaration.getNameAsString(), methodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                    .setParameters(methodDeclaration.getParameters())
                                    .setType(methodDeclaration.getType())
                                    .setAnnotations(methodDeclaration.getAnnotations())
                                    .setBody(methodDeclaration.getBody().orElseThrow());
                        }
                );

        componentProxyCompilationUnits
                .forEach(componentProxyCompilationUnit ->
                        componentProxyCompilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                .forEach(componentProxyClassDeclaration -> {
                                            MethodDeclaration superTypeMethodDeclaration = moduleCLassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .filter(declaration -> declaration.getType().isClassOrInterfaceType())
                                                    .filter(declaration -> declaration.getType().asClassOrInterfaceType().getNameAsString().equals(componentProxyClassDeclaration.getExtendedTypes(0).getNameAsString()))
                                                    .findFirst()
                                                    .orElseThrow();

                                            ConstructorDeclaration injectConstructorDeclaration = componentProxyClassDeclaration.getConstructors().stream()
                                                    .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                                                    .findFirst().orElseThrow();

                                            NodeList<Parameter> injectConstructorDeclarationParameters = injectConstructorDeclaration.getParameters();

                                            MethodDeclaration proxyMethodDeclaration = moduleProxyClassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(superTypeMethodDeclaration.getNameAsString()))
                                                    .filter(methodDeclaration -> methodDeclaration.getParameters().toString().equals(superTypeMethodDeclaration.getParameters().toString()))
                                                    .findFirst()
                                                    .orElseThrow();

                                            proxyMethodDeclaration.getBody()
                                                    .orElseThrow()
                                                    .getStatements()
                                                    .forEach(statement -> {
                                                                if (statement.isReturnStmt()) {
                                                                    Expression expression = statement.asReturnStmt().getExpression().orElseThrow();
                                                                    if (expression.isObjectCreationExpr()) {
                                                                        ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                                        objectCreationExpr.setType(componentProxyClassDeclaration.getNameAsString());

                                                                        injectConstructorDeclarationParameters.stream().skip(objectCreationExpr.getArguments().size())
                                                                                .forEach(parameter ->
                                                                                        {
                                                                                            MethodDeclaration interceptorBeanMethodDeclaration = interceptorBeanMethodDeclarations.stream()
                                                                                                    .filter(methodDeclaration ->
                                                                                                            methodDeclaration.getType().asClassOrInterfaceType().getNameAsString().equals(parameter.getType().asClassOrInterfaceType().getNameAsString())
                                                                                                    )
                                                                                                    .findFirst()
                                                                                                    .orElseThrow();

                                                                                            MethodCallExpr methodCallExpr = new MethodCallExpr().setName(interceptorBeanMethodDeclaration.getNameAsString());
                                                                                            interceptorBeanMethodDeclaration.getParameters()
                                                                                                    .forEach(interceptorBeanMethodParameter -> {
                                                                                                                proxyMethodDeclaration.addParameter(interceptorBeanMethodParameter);
                                                                                                                methodCallExpr.addArgument(interceptorBeanMethodParameter.getNameAsExpression());
                                                                                                            }
                                                                                                    );
                                                                                            objectCreationExpr.addArgument(methodCallExpr);
                                                                                        }
                                                                                );
                                                                    }
                                                                }
                                                            }
                                                    );
                                        }
                                )
                );
    }

    @Override
    public void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration moduleProxyClassDeclaration,
                                             CompilationUnit componentProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyClassDeclaration,
                                             CompilationUnit componentProxyComponentCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration) {

    }

    protected Optional<MethodDeclaration> getInterceptorBeanMethodDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, AnnotationExpr annotationExpr) {
        return getInterceptorBeanMethodDeclarations(classOrInterfaceDeclaration).stream()
                .filter(methodDeclaration ->
                        methodDeclaration.getAnnotationByClass(InterceptorBean.class)
                                .flatMap(methodAnnotationExpr ->
                                        methodAnnotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                                .findFirst()
                                )
                                .filter(memberValuePair -> memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType().getNameAsString().equals(annotationExpr.getNameAsString()))
                                .isPresent()
                )
                .findFirst();
    }

    protected List<MethodDeclaration> getInterceptorBeanMethodDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.getType().isClassOrInterfaceType())
                .collect(Collectors.toList());
    }
}
