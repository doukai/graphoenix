package io.graphoenix.chain;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.spi.patterns.ChainsBean;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class ChainProcessor implements DaggerProxyProcessor {

    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;

    @Override
    public void init(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType) {
        this.getCompilationUnitByClassOrInterfaceType = getCompilationUnitByClassOrInterfaceType;
    }

    @Override
    public void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                                    ClassOrInterfaceDeclaration moduleClassDeclaration,
                                    CompilationUnit componentCompilationUnit,
                                    ClassOrInterfaceDeclaration componentClassDeclaration,
                                    CompilationUnit componentProxyCompilationUnit,
                                    ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

    }

    @Override
    public Optional<CompilationUnit> createComponentProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        if (moduleBodyDeclaration.isMethodDeclaration()) {

            ClassOrInterfaceType type = getReturnTypeFromMethod(moduleBodyDeclaration.asMethodDeclaration()).orElseThrow();
            CompilationUnit chainsDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, type).orElseThrow();
            ClassOrInterfaceDeclaration chainsDefineClassOrInterfaceDeclaration = DAGGER_PROCESSOR_UTIL.getPublicClassOrInterfaceDeclaration(chainsDefineCompilationUnit).orElseThrow();

            ClassOrInterfaceDeclaration chainsImplDeclaration = new ClassOrInterfaceDeclaration()
                    .addModifier(Modifier.Keyword.PUBLIC)
                    .setName(type.getNameAsString() + "Impl");

            CompilationUnit chainsImplCompilationUnit = new CompilationUnit().addType(chainsImplDeclaration);
            chainsDefineCompilationUnit.getPackageDeclaration().ifPresent(chainsImplCompilationUnit::setPackageDeclaration);

            ConstructorDeclaration constructorDeclaration;
            BlockStmt constructorDeclarationBody;

            if (chainsDefineClassOrInterfaceDeclaration.isInterface()) {
                chainsImplDeclaration.addImplementedType(type);
                constructorDeclaration = chainsImplDeclaration.addConstructor(Modifier.Keyword.PUBLIC).addAnnotation(Inject.class);
                constructorDeclarationBody = constructorDeclaration.createBody();
            } else {
                chainsImplDeclaration.addExtendedType(type);
                constructorDeclaration = chainsImplDeclaration.getConstructors().stream()
                        .filter(injectConstructorDeclaration -> injectConstructorDeclaration.isAnnotationPresent(Inject.class)).findFirst()
                        .orElseGet(() -> chainsImplDeclaration.addConstructor(Modifier.Keyword.PUBLIC).addAnnotation(Inject.class));

                constructorDeclarationBody = chainsImplDeclaration.getConstructors().stream()
                        .filter(injectConstructorDeclaration -> injectConstructorDeclaration.isAnnotationPresent(Inject.class)).findFirst()
                        .map(ConstructorDeclaration::getBody)
                        .orElseGet(constructorDeclaration::createBody);
            }

            List<MethodDeclaration> methodDeclarationList = chainsDefineClassOrInterfaceDeclaration.getMembers().stream()
                    .filter(BodyDeclaration::isMethodDeclaration)
                    .map(BodyDeclaration::asMethodDeclaration)
                    .filter(methodDeclaration -> methodDeclaration.getBody().isEmpty())
                    .collect(Collectors.toList());

            methodDeclarationList.forEach(NodeWithOptionalBlockStmt::createBody);

            moduleBodyDeclaration.asMethodDeclaration().getBody().orElseThrow()
                    .getStatements().stream()
                    .filter(Statement::isExpressionStmt)
                    .map(Statement::asExpressionStmt)
                    .filter(expressionStmt -> expressionStmt.getExpression().isMethodCallExpr())
                    .map(expressionStmt -> expressionStmt.getExpression().asMethodCallExpr())
                    .forEach(methodCallExpr -> {
                        MethodDeclaration callMethodDeclaration = DAGGER_PROCESSOR_UTIL.getMethodDeclarationByMethodCallExpr(moduleClassDeclaration, moduleBodyDeclaration.asMethodDeclaration(), methodCallExpr);
                        chainsImplDeclaration.addField(callMethodDeclaration.getType(), callMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE);
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
                        methodDeclarationList.forEach(
                                methodDeclaration -> {
                                    BlockStmt blockStmt = methodDeclaration.getBody().orElseThrow();
                                    Expression argument;
                                    if (blockStmt.getStatements().size() == 0) {
                                        blockStmt.addStatement(
                                                new VariableDeclarationExpr().addVariable(
                                                        new VariableDeclarator()
                                                                .setType(methodDeclaration.getType())
                                                                .setName(methodCallExpr.getNameAsString().concat("Result"))
                                                                .setInitializer(
                                                                        new MethodCallExpr()
                                                                                .setName(methodDeclaration.getName())
                                                                                .setScope(methodCallExpr.getNameAsExpression())
                                                                                .setArguments(
                                                                                        new NodeList<>(methodDeclaration.getParameters().stream()
                                                                                                .map(NodeWithSimpleName::getNameAsExpression)
                                                                                                .collect(Collectors.toList())
                                                                                        )
                                                                                )
                                                                )
                                                )
                                        );
                                    } else {
                                        argument = blockStmt.getStatement(blockStmt.getStatements().size() - 1).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0).getNameAsExpression();
                                        blockStmt.addStatement(
                                                new VariableDeclarationExpr().addVariable(
                                                        new VariableDeclarator()
                                                                .setType(methodDeclaration.getType())
                                                                .setName(methodCallExpr.getNameAsString().concat("Result"))
                                                                .setInitializer(
                                                                        new MethodCallExpr()
                                                                                .setName(methodDeclaration.getName())
                                                                                .setScope(methodCallExpr.getNameAsExpression())
                                                                                .addArgument(argument)
                                                                )
                                                )
                                        );
                                    }
                                }
                        );
                    });

            methodDeclarationList.forEach(
                    methodDeclaration -> {
                        BlockStmt blockStmt = methodDeclaration.getBody().orElseThrow();
                        Expression argument = blockStmt.getStatement(blockStmt.getStatements().size() - 1).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0).getNameAsExpression();
                        blockStmt.addStatement(new ReturnStmt().setExpression(argument));
                        chainsImplDeclaration.addMember(methodDeclaration);
                    }
            );
            return Optional.of(chainsImplCompilationUnit);
        }
        return Optional.empty();
    }

    @Override
    public void prepareModuleProxy(BodyDeclaration<?> moduleBodyDeclaration,
                                   CompilationUnit moduleCompilationUnit,
                                   ClassOrInterfaceDeclaration moduleClassDeclaration,
                                   List<CompilationUnit> componentProxyCompilationUnits,
                                   CompilationUnit moduleProxyCompilationUnit,
                                   ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        if (moduleBodyDeclaration.isMethodDeclaration()) {

            MethodDeclaration originalMethodDeclaration = moduleBodyDeclaration.asMethodDeclaration();
            ClassOrInterfaceType originalMethodReturnType = getReturnTypeFromMethod(originalMethodDeclaration).orElseThrow();
            CompilationUnit chainsDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, originalMethodReturnType).orElseThrow();
            chainsDefineCompilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> moduleProxyCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(originalMethodReturnType + "ImplProxy")));

            MethodDeclaration moduleProxyMethodDeclaration = new MethodDeclaration()
                    .setName(originalMethodDeclaration.getName())
                    .setParameters(originalMethodDeclaration.getParameters())
                    .setAnnotations(originalMethodDeclaration.getAnnotations())
                    .setType(originalMethodReturnType.getNameAsString() + "ImplProxy");

            moduleProxyMethodDeclaration.getAnnotationByClass(ChainsBean.class).ifPresent(Node::remove);
            BlockStmt body = moduleProxyMethodDeclaration.createBody();
            ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr().setType(originalMethodReturnType + "ImplProxy");

            DAGGER_PROCESSOR_UTIL.getPublicClassOrInterfaceDeclaration(chainsDefineCompilationUnit).orElseThrow()
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
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleCLassDeclaration,
                                 List<CompilationUnit> componentProxyCompilationUnits,
                                 CompilationUnit moduleProxyCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {


    }

    @Override
    public void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration moduleProxyClassDeclaration,
                                             CompilationUnit componentProxyCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyClassDeclaration,
                                             CompilationUnit componentProxyComponentCompilationUnit,
                                             ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration) {

    }

    @Override
    public Class<? extends Annotation> support() {
        return ChainsBean.class;
    }

    public Optional<ClassOrInterfaceType> getReturnTypeFromMethod(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody().orElseThrow().getStatements().stream()
                .filter(Statement::isReturnStmt)
                .map(Statement::asReturnStmt)
                .filter(returnStmt -> returnStmt.getExpression().isPresent())
                .map(returnStmt -> returnStmt.getExpression().get())
                .filter(Expression::isClassExpr)
                .map(Expression::asClassExpr)
                .map(ClassExpr::getType)
                .filter(Type::isClassOrInterfaceType)
                .map(Type::asClassOrInterfaceType)
                .findFirst();
    }
}
