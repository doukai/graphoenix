package io.graphoenix.aop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.dagger.ProcessorManager;
import io.graphoenix.spi.aop.*;

import javax.inject.Provider;
import javax.tools.FileObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static io.graphoenix.dagger.JavaParserUtil.JAVA_PARSER_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class InterceptorProcessor implements DaggerProxyProcessor {

    private ProcessorManager processorManager;

    private static Set<String> aspectNames;

    @Override
    public void init(ProcessorManager processorManager) {
        this.processorManager = processorManager;
        aspectNames = new HashSet<>();
        try {
            Iterator<URL> urlIterator = Objects.requireNonNull(InterceptorProcessor.class.getClassLoader().getResources("META-INF/aspect/annotations.txt")).asIterator();
            while (urlIterator.hasNext()) {
                URL url = urlIterator.next();
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        aspectNames.add(line);
                    }
                }
            }
            Optional<FileObject> fileObject = processorManager.getResource("META-INF/aspect/annotations.txt");
            if (fileObject.isPresent()) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileObject.get().openInputStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        aspectNames.add(line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                                    ClassOrInterfaceDeclaration moduleClassDeclaration,
                                    CompilationUnit componentCompilationUnit,
                                    ClassOrInterfaceDeclaration componentClassDeclaration,
                                    CompilationUnit componentProxyCompilationUnit,
                                    ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

        componentClassDeclaration.getMembers().stream()
                .filter(bodyDeclaration -> !bodyDeclaration.isConstructorDeclaration())
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .forEach(methodDeclaration -> {
                            List<AnnotationExpr> interceptorAnnotationExprList = getMethodDeclarationInterceptorAnnotationExprList(componentCompilationUnit, methodDeclaration);
                            if (interceptorAnnotationExprList.size() > 0) {
                                interceptorAnnotationExprList
                                        .forEach(annotationExpr -> {
                                                    String interceptorFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, annotationExpr.getNameAsString()).concat("Interceptor");
                                                    if (componentProxyClassDeclaration.getFieldByName(interceptorFieldName).isEmpty()) {
                                                        componentProxyClassDeclaration.addField(new ClassOrInterfaceType().setName("Provider").setTypeArguments(new ClassOrInterfaceType().setName("Interceptor")), interceptorFieldName, Modifier.Keyword.PRIVATE);

                                                        componentClassDeclaration.getConstructors()
                                                                .forEach(constructorDeclaration ->
                                                                        componentProxyClassDeclaration.getConstructors()
                                                                                .forEach(componentProxyClassConstructor -> {
                                                                                            componentProxyClassConstructor.getBody()
                                                                                                    .addStatement(new AssignExpr()
                                                                                                            .setTarget(new FieldAccessExpr().setName(interceptorFieldName).setScope(new ThisExpr()))
                                                                                                            .setOperator(ASSIGN)
                                                                                                            .setValue(new MethodCallExpr()
                                                                                                                    .setName("getProvider")
                                                                                                                    .addArgument(new ClassExpr().setType(annotationExpr.getNameAsString()))
                                                                                                                    .setScope(new NameExpr().setName("InterceptorBeanContext"))
                                                                                                            )
                                                                                                    );
                                                                                        }
                                                                                )
                                                                );
                                                    }
                                                }
                                        );

                                BlockStmt blockStmt = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString(), methodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                        .setParameters(methodDeclaration.getParameters())
                                        .setType(methodDeclaration.getType())
                                        .addAnnotation(Override.class)
                                        .createBody();

                                interceptorAnnotationExprList
                                        .forEach(annotationExpr -> {
                                                    String interceptorFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, annotationExpr.getNameAsString()).concat("Interceptor");
                                                    Expression createContextExpr = new MethodCallExpr()
                                                            .setName("setOwner")
                                                            .addArgument(new ClassExpr().setType(annotationExpr.getName().getIdentifier()))
                                                            .setScope(
                                                                    new MethodCallExpr()
                                                                            .setName("setTarget")
                                                                            .addArgument(new ThisExpr())
                                                                            .setScope(
                                                                                    new MethodCallExpr()
                                                                                            .setName("setName")
                                                                                            .addArgument(new StringLiteralExpr(methodDeclaration.getNameAsString()))
                                                                                            .setScope(new ObjectCreationExpr().setType(InvocationContext.class))
                                                                            )
                                                            );

                                                    Expression addParameterValueExpr = methodDeclaration.getParameters().stream()
                                                            .map(parameter -> (Expression) parameter.getNameAsExpression())
                                                            .reduce(createContextExpr, (pre, current) ->
                                                                    new MethodCallExpr().setName("addParameterValue")
                                                                            .addArgument(new StringLiteralExpr(current.asNameExpr().getNameAsString()))
                                                                            .addArgument(current)
                                                                            .setScope(pre)
                                                            );

                                                    Expression addOwnerValueExpr = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
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
                                                                    .setName(interceptorFieldName.concat("Context"))
                                                                    .setInitializer(addOwnerValueExpr)
                                                            );

                                                    blockStmt.addStatement(variableDeclarationExpr);
                                                    blockStmt.addStatement(new MethodCallExpr().setName("before").addArgument(interceptorFieldName.concat("Context")).setScope(new MethodCallExpr().setName("get").setScope(new NameExpr().setName(interceptorFieldName))));
                                                }
                                        );

                                if (!methodDeclaration.getType().isVoidType()) {
                                    VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr().addVariable(
                                            new VariableDeclarator()
                                                    .setName("result")
                                                    .setType(methodDeclaration.getType())
                                                    .setInitializer(
                                                            new MethodCallExpr()
                                                                    .setName(methodDeclaration.getName())
                                                                    .setArguments(methodDeclaration.getParameters().stream().map(NodeWithSimpleName::getNameAsExpression).collect(Collectors.toCollection(NodeList::new)))
                                                                    .setScope(new SuperExpr())
                                                    ));

                                    blockStmt.addStatement(variableDeclarationExpr);
                                }

                                interceptorAnnotationExprList
                                        .forEach(annotationExpr -> {
                                                    String interceptorFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, annotationExpr.getNameAsString()).concat("Interceptor");
                                                    if (!methodDeclaration.getType().isVoidType()) {
                                                        blockStmt.addStatement(new MethodCallExpr().setName("setReturnValue").addArgument("result").setScope(new NameExpr(interceptorFieldName.concat("Context"))));
                                                    }

                                                    blockStmt.addStatement(new MethodCallExpr().setName("after").addArgument(interceptorFieldName.concat("Context")).setScope(new MethodCallExpr().setName("get").setScope(new NameExpr().setName(interceptorFieldName))));

                                                    if (!methodDeclaration.getType().isVoidType()) {
                                                        blockStmt.addStatement(new ReturnStmt().setExpression(new NameExpr("result")));
                                                    }
                                                }
                                        );
                            }
                        }
                );

        componentProxyCompilationUnit.addImport(Provider.class).addImport(Interceptor.class).addImport(InvocationContext.class).addImport(InvocationContext.class).addImport("io.graphoenix.core.aop.InterceptorBeanContext");
    }

    @Override
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleClassDeclaration,
                                 List<CompilationUnit> componentProxyCompilationUnits,
                                 CompilationUnit moduleProxyCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        if (moduleClassDeclaration.isInterface()) {
            return;
        }

        List<MethodDeclaration> interceptorBeanMethodDeclarations = getInterceptorBeanMethodDeclarations(moduleClassDeclaration);

        interceptorBeanMethodDeclarations
                .forEach(methodDeclaration -> {
                            moduleProxyClassDeclaration
                                    .addMethod(methodDeclaration.getNameAsString(), methodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                    .setParameters(methodDeclaration.getParameters())
                                    .setType(methodDeclaration.getType())
                                    .setAnnotations(methodDeclaration.getAnnotations().stream().filter(annotationExpr -> !annotationExpr.getNameAsString().equals(InterceptorBean.class.getSimpleName())).collect(Collectors.toCollection(NodeList::new)))
                                    .setBody(methodDeclaration.getBody().orElseThrow());

                            processorManager.getCompilationUnitByClassOrInterfaceType(methodDeclaration.getType().asClassOrInterfaceType())
                                    .ifPresent(componentProxyCompilationUnits::add);
                        }
                );

        processorManager.writeToFiler(buildModuleContext(moduleCompilationUnit, interceptorBeanMethodDeclarations, moduleProxyCompilationUnit, moduleProxyClassDeclaration));
    }

    @Override
    public void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration moduleProxyClassDeclaration,
                                             CompilationUnit componentProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyClassDeclaration,
                                             CompilationUnit componentProxyComponentCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration) {
    }

    protected CompilationUnit buildModuleContext(CompilationUnit moduleCompilationUnit, List<MethodDeclaration> interceptorBeanMethodDeclarations, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleProxyClassDeclaration.getNameAsString().concat("InterceptorBeanContext"))
                .addAnnotation(
                        new SingleMemberAnnotationExpr()
                                .setMemberValue(new ClassExpr().setType(InterceptorBeanModuleContext.class))
                                .setName(AutoService.class.getSimpleName())
                )
                .addExtendedType(BaseInterceptorBeanModuleContext.class);

        CompilationUnit moduleContextComponentCompilationUnit = new CompilationUnit()
                .addType(moduleContextInterfaceDeclaration)
                .addImport(AutoService.class)
                .addImport(InterceptorBeanModuleContext.class)
                .addImport(BaseInterceptorBeanModuleContext.class);

        moduleProxyCompilationUnit.getPackageDeclaration().ifPresent(moduleContextComponentCompilationUnit::setPackageDeclaration);

        BlockStmt blockStmt = moduleContextInterfaceDeclaration.addStaticInitializer();

        interceptorBeanMethodDeclarations.forEach(
                interceptorBeanMethodDeclaration -> {
                    String beanClassName = interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType().getNameAsString();
                    String componentClassName = beanClassName.concat("Component");
                    String daggerClassName = "Dagger".concat(componentClassName);
                    String daggerVariableName = "dagger".concat(componentClassName);

                    moduleContextComponentCompilationUnit.addImport(processorManager.getNameByType(interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()));

                    blockStmt.addStatement(new VariableDeclarationExpr()
                            .addVariable(
                                    new VariableDeclarator()
                                            .setType(componentClassName)
                                            .setName(daggerVariableName)
                                            .setInitializer(
                                                    new MethodCallExpr()
                                                            .setName("create")
                                                            .setScope(new NameExpr().setName(daggerClassName))
                                            )
                            )
                    );

                    getInterceptorAnnotationClassExprList(interceptorBeanMethodDeclaration)
                            .forEach(classExpr -> {
                                        blockStmt.addStatement(
                                                new MethodCallExpr()
                                                        .setName("put")
                                                        .addArgument(classExpr)
                                                        .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
                                        );
                                        moduleContextComponentCompilationUnit.addImport(processorManager.getNameByType(classExpr.getType().asClassOrInterfaceType()));
                                    }
                            );

                    String interceptorBeanName = processorManager.getNameByType(interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType());
                    moduleContextComponentCompilationUnit.addImport(interceptorBeanName.replace(beanClassName, componentClassName));
                    moduleContextComponentCompilationUnit.addImport(interceptorBeanName.replace(beanClassName, daggerClassName));
                }
        );

        return moduleContextComponentCompilationUnit;
    }


    @Override
    public Class<? extends Annotation> support() {
        return InterceptorBean.class;
    }

    protected List<AnnotationExpr> getMethodDeclarationInterceptorAnnotationExprList(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
        return methodDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> isInterceptorAnnotation(compilationUnit, annotationExpr))
                .collect(Collectors.toList());
    }

    protected boolean isInterceptorAnnotation(CompilationUnit compilationUnit, AnnotationExpr annotationExpr) {
        return processorManager.getCompilationUnitByAnnotationExpr(annotationExpr)
                .flatMap(JAVA_PARSER_UTIL::getPublicAnnotationDeclaration)
                .filter(annotationDeclaration ->
                        aspectNames.contains(annotationDeclaration.getFullyQualifiedName().orElse(annotationDeclaration.getNameAsString())) ||
                                annotationDeclaration.isAnnotationPresent(Aspect.class)
                )
                .isPresent();
    }

    protected List<MethodDeclaration> getInterceptorBeanMethodDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.getType().isClassOrInterfaceType())
                .collect(Collectors.toList());
    }

    private List<ClassExpr> getInterceptorAnnotationClassExprList(MethodDeclaration interceptorBeanMethodDeclaration) {
        return interceptorBeanMethodDeclaration
                .getAnnotationByClass(InterceptorBean.class)
                .orElseThrow()
                .asNormalAnnotationExpr()
                .getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                .flatMap(memberValuePair -> {
                            if (memberValuePair.getValue().isArrayInitializerExpr()) {
                                return memberValuePair.getValue().asArrayInitializerExpr().getValues().stream().map(Expression::asClassExpr);
                            } else {
                                return Stream.of(memberValuePair.getValue().asClassExpr());
                            }
                        }
                )
                .collect(Collectors.toList());
    }
}
