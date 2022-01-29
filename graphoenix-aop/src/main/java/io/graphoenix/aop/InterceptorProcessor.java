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
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.dagger.ProcessorTools;
import io.graphoenix.spi.aop.BaseInterceptorBeanModuleContext;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.aop.InterceptorBeanModuleContext;
import io.graphoenix.spi.aop.InvocationContext;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;

@AutoService(DaggerProxyProcessor.class)
public class InterceptorProcessor implements DaggerProxyProcessor {

    private ProcessorTools processorTools;

    @Override
    public void init(ProcessorTools processorTools) {
        this.processorTools = processorTools;
        interceptorAnnotations = new HashMap<>();
    }

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

        interceptorBeanMethodDeclarations
                .forEach(interceptorBeanMethodDeclaration ->
                        interceptorAnnotations.put(
                                getInterceptorAnnotationClassName(moduleCompilationUnit, interceptorBeanMethodDeclaration),
                                getInterceptorAnnotationClassName(moduleCompilationUnit, interceptorBeanMethodDeclaration)
                        )
                );

        interceptorBeanMethodDeclarations.forEach(interceptorBeanMethodDeclaration -> componentProxyClassDeclaration.addField(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE));

        componentClassDeclaration.getConstructors()
                .forEach(constructorDeclaration ->
                        componentProxyClassDeclaration.getConstructors()
                                .forEach(componentProxyClassConstructor -> {
                                            interceptorBeanMethodDeclarations
                                                    .forEach(interceptorBeanMethodDeclaration ->
                                                            componentProxyClassConstructor.getBody()
                                                                    .addStatement(new AssignExpr()
                                                                            .setTarget(new FieldAccessExpr().setName(interceptorBeanMethodDeclaration.getName()).setScope(new ThisExpr()))
                                                                            .setOperator(ASSIGN)
                                                                            .setValue(new MethodCallExpr()
                                                                                    .setName("get")
                                                                                    .addArgument(getInterceptorAnnotationClassExpr(interceptorBeanMethodDeclaration))
                                                                                    .setScope(new NameExpr().setName("InterceptorBeanContext"))
                                                                            )
                                                                    )
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

        componentProxyCompilationUnit.addImport(InvocationContext.class).addImport("io.graphoenix.core.aop.InterceptorBeanContext");
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

                            processorTools.getGetCompilationUnitByClassOrInterfaceType().apply(moduleCompilationUnit, methodDeclaration.getType().asClassOrInterfaceType())
                                    .ifPresent(componentProxyCompilationUnits::add);
                        }
                );

        processorTools.getWriteToFiler().accept(buildModuleContext(moduleCompilationUnit, interceptorBeanMethodDeclarations, moduleProxyCompilationUnit, moduleProxyClassDeclaration));
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

                    moduleContextComponentCompilationUnit.addImport(processorTools.getGetTypeNameByClassOrInterfaceType().apply(moduleCompilationUnit, interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()).orElseThrow());

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

                    ClassExpr interceptorAnnotationClassExpr = getInterceptorAnnotationClassExpr(interceptorBeanMethodDeclaration);
                    blockStmt.addStatement(
                            new MethodCallExpr()
                                    .setName("put")
                                    .addArgument(interceptorAnnotationClassExpr)
                                    .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
                    );

                    moduleContextComponentCompilationUnit.addImport(processorTools.getGetTypeNameByClassOrInterfaceType().apply(moduleCompilationUnit, interceptorAnnotationClassExpr.getType().asClassOrInterfaceType()).orElseThrow());
                    String interceptorBeanName = processorTools.getGetTypeNameByClassOrInterfaceType().apply(moduleCompilationUnit, interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()).orElseThrow();
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

    private ClassExpr getInterceptorAnnotationClassExpr(MethodDeclaration interceptorBeanMethodDeclaration) {
        return interceptorBeanMethodDeclaration
                .getAnnotationByClass(InterceptorBean.class)
                .orElseThrow()
                .asNormalAnnotationExpr()
                .getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                .map(memberValuePair -> memberValuePair.getValue().asClassExpr())
                .findFirst()
                .orElseThrow();
    }

    private String getInterceptorAnnotationClassName(CompilationUnit moduleCompilationUnit, MethodDeclaration interceptorBeanMethodDeclaration) {
        return processorTools.getGetTypeNameByClassOrInterfaceType().apply(moduleCompilationUnit, getInterceptorAnnotationClassExpr(interceptorBeanMethodDeclaration).getType().asClassOrInterfaceType()).orElseThrow();
    }
}
