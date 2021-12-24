package io.graphoenix.chain;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
            ClassOrInterfaceDeclaration chainsDefineClassOrInterfaceDeclaration = chainsDefineCompilationUnit.getTypes().stream()
                    .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                    .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                    .findFirst()
                    .orElseThrow();

            ClassOrInterfaceDeclaration chainsImplDeclaration = new ClassOrInterfaceDeclaration()
                    .addModifier(Modifier.Keyword.PUBLIC)
                    .setName(type.getNameAsString() + "Impl");

            CompilationUnit chainsImplCompilationUnit = new CompilationUnit().addType(chainsImplDeclaration);
            chainsDefineCompilationUnit.getPackageDeclaration().ifPresent(chainsImplCompilationUnit::setPackageDeclaration);

            if (chainsDefineClassOrInterfaceDeclaration.isInterface()) {
                chainsImplDeclaration.addImplementedType(type);
            } else {
                chainsImplDeclaration.addExtendedType(type);
            }

            ConstructorDeclaration constructorDeclaration = chainsImplDeclaration.addConstructor(Modifier.Keyword.PUBLIC)
                    .addAnnotation(Inject.class)
                    .setParameters(moduleBodyDeclaration.asMethodDeclaration().getParameters());

            BlockStmt constructorDeclarationBody = constructorDeclaration.createBody();

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
                                                new VariableDeclarationExpr()
                                                        .addVariable(
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
                                                new VariableDeclarationExpr()
                                                        .addVariable(
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
