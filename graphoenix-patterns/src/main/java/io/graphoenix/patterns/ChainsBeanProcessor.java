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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.spi.patterns.ChainsBean;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class ChainsBeanProcessor implements DaggerProxyProcessor {

    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;

    @Override
    public void init(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType,
                     BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource) {
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
            MethodDeclaration originalMethodDeclaration = moduleBodyDeclaration.asMethodDeclaration();

            ClassOrInterfaceType type = DAGGER_PROCESSOR_UTIL.getReturnTypeFromMethod(originalMethodDeclaration).orElseThrow();
            CompilationUnit chainsDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, type).orElseThrow();
            ClassOrInterfaceDeclaration chainsDefineClassOrInterfaceDeclaration = DAGGER_PROCESSOR_UTIL.getPublicClassOrInterfaceDeclaration(chainsDefineCompilationUnit).orElseThrow();

            ClassOrInterfaceDeclaration chainsImplDeclaration = new ClassOrInterfaceDeclaration()
                    .addModifier(Modifier.Keyword.PUBLIC)
                    .removeModifier(Modifier.Keyword.ABSTRACT)
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

            originalMethodDeclaration.getBody().orElseThrow()
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
                                    methodDeclaration.getAnnotations().clear();
                                    methodDeclaration.addAnnotation(Override.class);
                                    BlockStmt blockStmt = methodDeclaration.getBody().orElseThrow();

                                    NodeList<Expression> methodParameterNameList = new NodeList<>(methodDeclaration.getParameters().stream()
                                            .map(NodeWithSimpleName::getNameAsExpression)
                                            .collect(Collectors.toList())
                                    );

                                    MethodCallExpr stepMethodCallExpr = new MethodCallExpr()
                                            .setName(methodDeclaration.getName())
                                            .setScope(methodCallExpr.getNameAsExpression());

                                    Optional<NameExpr> lastReturnVariable = getLastReturnVariable(methodDeclaration);
                                    if (lastReturnVariable.isPresent()) {
                                        stepMethodCallExpr.addArgument(lastReturnVariable.get());
                                    } else {
                                        stepMethodCallExpr.setArguments(methodParameterNameList);
                                    }

                                    if (callMethodDeclaration.getType().isVoidType()) {
                                        blockStmt.addStatement(stepMethodCallExpr);
                                    } else {
                                        blockStmt.addStatement(
                                                new VariableDeclarationExpr().addVariable(
                                                        new VariableDeclarator()
                                                                .setType(methodDeclaration.getType())
                                                                .setName(methodCallExpr.getNameAsString().concat("Result"))
                                                                .setInitializer(stepMethodCallExpr)
                                                )
                                        );
                                    }
                                }
                        );
                    });

            methodDeclarationList.forEach(
                    methodDeclaration -> {
                        if (!methodDeclaration.getType().isVoidType()) {
                            BlockStmt blockStmt = methodDeclaration.getBody().orElseThrow();
                            Expression argument = getLastReturnVariable(methodDeclaration).orElseGet(() -> methodDeclaration.getParameter(0).getNameAsExpression());
                            blockStmt.addStatement(new ReturnStmt().setExpression(argument));
                            chainsImplDeclaration.addMember(methodDeclaration);
                        }
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
            ClassOrInterfaceType originalMethodReturnType = DAGGER_PROCESSOR_UTIL.getReturnTypeFromMethod(originalMethodDeclaration).orElseThrow();
            CompilationUnit chainsDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, originalMethodReturnType).orElseThrow();
            chainsDefineCompilationUnit.getPackageDeclaration()
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

            moduleProxyMethodDeclaration.getAnnotationByClass(ChainsBean.class).ifPresent(Node::remove);
            BlockStmt body = moduleProxyMethodDeclaration.createBody();
            ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr().setType(originalMethodReturnType.getNameAsString() + "ImplProxy");

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

    private Optional<NameExpr> getLastReturnVariable(MethodDeclaration methodDeclaration) {
        BlockStmt blockStmt = methodDeclaration.getBody().orElseThrow();
        int size = blockStmt.getStatements().size();
        return IntStream.range(0, size)
                .mapToObj(index -> blockStmt.getStatement(size - 1 - index))
                .filter(Statement::isExpressionStmt)
                .map(Statement::asExpressionStmt)
                .filter(expressionStmt -> expressionStmt.getExpression().isVariableDeclarationExpr())
                .map(expressionStmt -> expressionStmt.getExpression().asVariableDeclarationExpr())
                .map(variableDeclarationExpr -> variableDeclarationExpr.getVariable(0).getNameAsExpression())
                .findFirst();
    }
}
