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
import com.google.common.base.CaseFormat;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.dagger.ProcessorTools;
import io.graphoenix.spi.aop.*;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class InterceptorProcessor implements DaggerProxyProcessor {

    private ProcessorTools processorTools;

    @Override
    public void init(ProcessorTools processorTools) {
        this.processorTools = processorTools;
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
                                BlockStmt blockStmt = componentProxyClassDeclaration.addMethod(methodDeclaration.getNameAsString(), methodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                        .setParameters(methodDeclaration.getParameters())
                                        .setType(methodDeclaration.getType())
                                        .addAnnotation(Override.class)
                                        .createBody();

                                interceptorAnnotationExprList
                                        .forEach(annotationExpr -> {
                                                    String interceptorFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, annotationExpr.getNameAsString()).concat("Interceptor");
                                                    if (componentProxyClassDeclaration.getFieldByName(interceptorFieldName).isEmpty()) {
                                                        componentProxyClassDeclaration.addField(Interceptor.class, interceptorFieldName, Modifier.Keyword.PRIVATE);

                                                        componentClassDeclaration.getConstructors()
                                                                .forEach(constructorDeclaration ->
                                                                        componentProxyClassDeclaration.getConstructors()
                                                                                .forEach(componentProxyClassConstructor -> {
                                                                                            componentProxyClassConstructor.getBody()
                                                                                                    .addStatement(new AssignExpr()
                                                                                                            .setTarget(new FieldAccessExpr().setName(interceptorFieldName).setScope(new ThisExpr()))
                                                                                                            .setOperator(ASSIGN)
                                                                                                            .setValue(new MethodCallExpr()
                                                                                                                    .setName("get")
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
                                                    blockStmt.addStatement(new MethodCallExpr().setName("before").addArgument(interceptorFieldName.concat("Context")).setScope(new NameExpr().setName(interceptorFieldName)));
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

                                                    blockStmt.addStatement(new MethodCallExpr().setName("after").addArgument(interceptorFieldName.concat("Context")).setScope(new NameExpr().setName(interceptorFieldName)));

                                                    if (!methodDeclaration.getType().isVoidType()) {
                                                        blockStmt.addStatement(new ReturnStmt().setExpression(new NameExpr("result")));
                                                    }
                                                }
                                        );
                            }
                        }
                );

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

                    getInterceptorAnnotationClassExprList(interceptorBeanMethodDeclaration)
                            .forEach(classExpr -> {
                                        blockStmt.addStatement(
                                                new MethodCallExpr()
                                                        .setName("put")
                                                        .addArgument(classExpr)
                                                        .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
                                        );
                                        moduleContextComponentCompilationUnit.addImport(processorTools.getGetTypeNameByClassOrInterfaceType().apply(moduleCompilationUnit, classExpr.getType().asClassOrInterfaceType()).orElseThrow());
                                    }
                            );

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

    protected List<AnnotationExpr> getMethodDeclarationInterceptorAnnotationExprList(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
        return methodDeclaration.getAnnotations().stream()
                .filter(annotationExpr -> isInterceptorAnnotation(compilationUnit, annotationExpr))
                .collect(Collectors.toList());
    }

    protected boolean isInterceptorAnnotation(CompilationUnit compilationUnit, AnnotationExpr annotationExpr) {
        return processorTools.getGetCompilationUnitByClassOrInterfaceTypeName().apply(compilationUnit, annotationExpr.getNameAsString())
                .flatMap(DAGGER_PROCESSOR_UTIL::getPublicAnnotationDeclaration)
                .filter(annotationDeclaration -> annotationDeclaration.isAnnotationPresent(InterceptorAnnotation.class))
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
