package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum DaggerProcessorUtil {
    DAGGER_PROCESSOR_UTIL;

    public ClassOrInterfaceType getMethodReturnType(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody().orElseThrow()
                .getStatements().stream()
                .filter(Statement::isReturnStmt)
                .map(Statement::asReturnStmt)
                .map(ReturnStmt::getExpression)
                .map(Optional::orElseThrow)
                .filter(Expression::isObjectCreationExpr)
                .map(Expression::asObjectCreationExpr)
                .map(ObjectCreationExpr::getType)
                .findFirst()
                .orElseThrow();
    }

    public List<MethodDeclaration> findReferenceMethodDeclarations(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst()
                .orElseThrow()
                .getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .filter(referenceMethodDeclaration ->
                        referenceMethodDeclaration.getBody().orElseThrow()
                                .getStatements().stream()
                                .filter(Statement::isReturnStmt)
                                .map(Statement::asReturnStmt)
                                .map(statement -> statement.getExpression().orElseThrow())
                                .filter(Expression::isObjectCreationExpr)
                                .map(Expression::asObjectCreationExpr)
                                .anyMatch(objectCreationExpr ->
                                        objectCreationExpr.getArguments()
                                                .stream()
                                                .filter(Expression::isMethodCallExpr)
                                                .map(Expression::asMethodCallExpr)
                                                .anyMatch(methodCallExpr -> methodCallExpr.getNameAsString().equals(methodDeclaration.getNameAsString()))
                                )
                ).collect(Collectors.toList());
    }

    public void addReferenceMethodDeclarationParameter(MethodDeclaration methodDeclaration, MethodDeclaration referenceMethodDeclaration, Parameter parameter) {
        referenceMethodDeclaration.addParameter(parameter);

        referenceMethodDeclaration.getBody().orElseThrow()
                .getStatements().stream()
                .filter(Statement::isReturnStmt)
                .map(Statement::asReturnStmt)
                .map(returnStmt -> returnStmt.getExpression().orElseThrow())
                .filter(Expression::isObjectCreationExpr)
                .map(Expression::asObjectCreationExpr)
                .findFirst()
                .orElseThrow()
                .getArguments().stream()
                .filter(Expression::isMethodCallExpr)
                .map(Expression::asMethodCallExpr)
                .filter(methodCallExpr -> methodCallExpr.getNameAsString().equals(methodDeclaration.getNameAsString()))
                .forEach(methodCallExpr -> methodCallExpr.addArgument(parameter.getNameAsExpression()));
    }


    public MethodDeclaration getMethodDeclarationByMethodCallExpr(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, MethodDeclaration containerMethodDeclaration, MethodCallExpr methodCallExpr) {

        NodeList<Node> parameterList = new NodeList<>();
        parameterList.addAll(methodCallExpr.getArguments().stream()
                .map(expression -> getParameterByArgument(containerMethodDeclaration, expression)).collect(Collectors.toList()));

        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodCallExpr.getNameAsString()))
                .filter(methodDeclaration -> methodDeclaration.getParameters().toString().equals(parameterList.toString()))
                .findFirst()
                .orElseThrow();
    }

    public Parameter getParameterByArgument(MethodDeclaration methodDeclaration, Expression expression) {
        return methodDeclaration.getParameters().stream().filter(parameter -> parameter.getNameAsExpression().equals(expression)).findFirst().orElseThrow();
    }

    public Optional<ClassOrInterfaceDeclaration> getPublicClassOrInterfaceDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst();
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
