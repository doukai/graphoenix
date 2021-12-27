package io.graphoenix.patterns;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.spi.patterns.CompositeBean;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class CompositeBeanProcessor implements DaggerProxyProcessor {

    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;
    private BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource;

    @Override
    public void init(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType, BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource) {
        this.getCompilationUnitByClassOrInterfaceType = getCompilationUnitByClassOrInterfaceType;
        this.importAllTypesFromSource = importAllTypesFromSource;
    }

    @Override
    public void buildComponentProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

    }

    @Override
    public Optional<CompilationUnit> createComponentProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        if (moduleBodyDeclaration.isMethodDeclaration()) {
            MethodDeclaration originalMethodDeclaration = moduleBodyDeclaration.asMethodDeclaration();

            ClassOrInterfaceType type = DAGGER_PROCESSOR_UTIL.getReturnTypeFromMethod(originalMethodDeclaration).orElseThrow();
            CompilationUnit compositeDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, type).orElseThrow();
            ClassOrInterfaceDeclaration compositeDefineClassOrInterfaceDeclaration = DAGGER_PROCESSOR_UTIL.getPublicClassOrInterfaceDeclaration(compositeDefineCompilationUnit).orElseThrow();
            ClassOrInterfaceDeclaration compositeImplDeclaration = new ClassOrInterfaceDeclaration()
                    .addModifier(Modifier.Keyword.PUBLIC)
                    .removeModifier(Modifier.Keyword.ABSTRACT)
                    .setName(type.getNameAsString() + "Impl");

            CompilationUnit compositeImplCompilationUnit = new CompilationUnit().addType(compositeImplDeclaration);
            compositeDefineCompilationUnit.getPackageDeclaration().ifPresent(compositeImplCompilationUnit::setPackageDeclaration);

            ConstructorDeclaration constructorDeclaration;
            BlockStmt constructorDeclarationBody;

            if (compositeDefineClassOrInterfaceDeclaration.isInterface()) {
                compositeImplDeclaration.addImplementedType(type);
                constructorDeclaration = compositeImplDeclaration.addConstructor(Modifier.Keyword.PUBLIC).addAnnotation(Inject.class);
                constructorDeclarationBody = constructorDeclaration.createBody();
            } else {
                compositeImplDeclaration.addExtendedType(type);
                constructorDeclaration = compositeImplDeclaration.getConstructors().stream()
                        .filter(injectConstructorDeclaration -> injectConstructorDeclaration.isAnnotationPresent(Inject.class)).findFirst()
                        .orElseGet(() -> compositeImplDeclaration.addConstructor(Modifier.Keyword.PUBLIC).addAnnotation(Inject.class));

                constructorDeclarationBody = compositeImplDeclaration.getConstructors().stream()
                        .filter(injectConstructorDeclaration -> injectConstructorDeclaration.isAnnotationPresent(Inject.class)).findFirst()
                        .map(ConstructorDeclaration::getBody)
                        .orElseGet(constructorDeclaration::createBody);
            }

            List<MethodDeclaration> compositeMethodDeclarationList = compositeDefineClassOrInterfaceDeclaration.getMembers().stream()
                    .filter(BodyDeclaration::isMethodDeclaration)
                    .map(BodyDeclaration::asMethodDeclaration)
                    .filter(methodDeclaration -> methodDeclaration.getBody().isEmpty())
                    .collect(Collectors.toList());

            originalMethodDeclaration.getBody().orElseThrow()
                    .getStatements().stream()
                    .filter(Statement::isExpressionStmt)
                    .map(Statement::asExpressionStmt)
                    .filter(expressionStmt -> expressionStmt.getExpression().isMethodCallExpr())
                    .map(expressionStmt -> expressionStmt.getExpression().asMethodCallExpr())
                    .forEach(methodCallExpr -> {
                        MethodDeclaration callMethodDeclaration = DAGGER_PROCESSOR_UTIL.getMethodDeclarationByMethodCallExpr(moduleClassDeclaration, moduleBodyDeclaration.asMethodDeclaration(), methodCallExpr);
                        compositeImplDeclaration.addField(callMethodDeclaration.getType(), callMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE);
                        constructorDeclaration.addParameter(
                                new Parameter()
                                        .setType(callMethodDeclaration.getType())
                                        .setName(methodCallExpr.getName())
                        );
                        constructorDeclarationBody.addStatement(
                                new AssignExpr()
                                        .setTarget(new FieldAccessExpr().setName(callMethodDeclaration.getName()).setScope(new ThisExpr()))
                                        .setOperator(ASSIGN)
                                        .setValue(callMethodDeclaration.getNameAsExpression())
                        );
                    });

            compositeMethodDeclarationList.forEach(
                    compositeMethodDeclaration -> {
                        originalMethodDeclaration.getBody().orElseThrow()
                                .getStatements().stream()
                                .filter(Statement::isExpressionStmt)
                                .map(Statement::asExpressionStmt)
                                .filter(expressionStmt -> expressionStmt.getExpression().isMethodCallExpr())
                                .map(expressionStmt -> expressionStmt.getExpression().asMethodCallExpr())
                                .forEach(componentMethodCallExpr -> {
                                            MethodDeclaration componentMethodDeclaration = DAGGER_PROCESSOR_UTIL.getMethodDeclarationByMethodCallExpr(moduleClassDeclaration, moduleBodyDeclaration.asMethodDeclaration(), componentMethodCallExpr);
                                            Optional<CompilationUnit> componentCompilationUnit = getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, componentMethodDeclaration.getType().asClassOrInterfaceType());
                                            componentCompilationUnit.flatMap(DAGGER_PROCESSOR_UTIL::getPublicClassOrInterfaceDeclaration)
                                                    .flatMap(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getMembers().stream()
                                                            .filter(BodyDeclaration::isMethodDeclaration)
                                                            .map(BodyDeclaration::asMethodDeclaration)
                                                            .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(compositeMethodDeclaration.getNameAsString()))
                                                            .filter(methodDeclaration -> methodDeclaration.getType().asString().equals(compositeMethodDeclaration.getType().asString()))
                                                            .filter(methodDeclaration -> DAGGER_PROCESSOR_UTIL.hasSameParameters(methodDeclaration.getParameters(), compositeMethodDeclaration.getParameters()))
                                                            .findFirst())
                                                    .ifPresent(methodDeclaration -> {
                                                        methodDeclaration.getAnnotations().clear();
                                                        methodDeclaration.addAnnotation(Override.class);
                                                        BlockStmt blockStmt = methodDeclaration.createBody();
                                                        MethodCallExpr methodCallExpr = new MethodCallExpr()
                                                                .setName(methodDeclaration.getNameAsString())
                                                                .setArguments(new NodeList<>(
                                                                                compositeMethodDeclaration.getParameters().stream()
                                                                                        .map(NodeWithSimpleName::getNameAsExpression)
                                                                                        .collect(Collectors.toList())
                                                                        )
                                                                )
                                                                .setScope(componentMethodCallExpr.getNameAsExpression());
                                                        if (methodDeclaration.getType().isVoidType()) {
                                                            blockStmt.addStatement(methodCallExpr);
                                                        } else {
                                                            blockStmt.addStatement(new ReturnStmt().setExpression(methodCallExpr));
                                                        }
                                                        compositeImplDeclaration.addMember(methodDeclaration);
                                                        importAllTypesFromSource.accept(compositeImplCompilationUnit, componentCompilationUnit.orElseThrow());
                                                    });
                                        }
                                );
                    }
            );
            return Optional.of(compositeImplCompilationUnit);
        }
        return Optional.empty();
    }

    @Override
    public void prepareModuleProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        if (moduleBodyDeclaration.isMethodDeclaration()) {

            MethodDeclaration originalMethodDeclaration = moduleBodyDeclaration.asMethodDeclaration();
            ClassOrInterfaceType originalMethodReturnType = DAGGER_PROCESSOR_UTIL.getReturnTypeFromMethod(originalMethodDeclaration).orElseThrow();
            CompilationUnit compositeDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, originalMethodReturnType).orElseThrow();
            compositeDefineCompilationUnit.getPackageDeclaration()
                    .ifPresent(packageDeclaration -> {
                                moduleProxyCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(originalMethodReturnType.getNameAsString()));
                                moduleProxyCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(originalMethodReturnType.getNameAsString() + "ImplProxy"));
                            }
                    );
            MethodDeclaration moduleProxyMethodDeclaration = new MethodDeclaration()
                    .setName(originalMethodDeclaration.getName())
                    .setParameters(originalMethodDeclaration.getParameters())
                    .setAnnotations(originalMethodDeclaration.getAnnotations())
                    .setType(originalMethodReturnType.getNameAsString());

            moduleProxyMethodDeclaration.getAnnotationByClass(CompositeBean.class).ifPresent(Node::remove);
            BlockStmt body = moduleProxyMethodDeclaration.createBody();
            ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr().setType(originalMethodReturnType.getNameAsString() + "ImplProxy");

            DAGGER_PROCESSOR_UTIL.getPublicClassOrInterfaceDeclaration(compositeDefineCompilationUnit).orElseThrow()
                    .getConstructors().stream()
                    .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                    .findFirst()
                    .ifPresent(constructorDeclaration ->
                            constructorDeclaration.getParameters().forEach(parameter -> objectCreationExpr.addArgument(parameter.getNameAsExpression()))
                    );

            originalMethodDeclaration.getBody().orElseThrow()
                    .getStatements().stream()
                    .filter(Statement::isExpressionStmt)
                    .map(statement -> statement.asExpressionStmt().getExpression())
                    .filter(Expression::isMethodCallExpr)
                    .map(Expression::asMethodCallExpr)
                    .forEach(objectCreationExpr::addArgument);

            body.addStatement(new ReturnStmt().setExpression(objectCreationExpr));
            moduleProxyClassDeclaration.addMember(moduleProxyMethodDeclaration);
        }
    }

    @Override
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

    }

    @Override
    public void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, CompilationUnit componentProxyComponentCompilationUnit, ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration) {

    }

    @Override
    public Class<? extends Annotation> support() {
        return CompositeBean.class;
    }
}
