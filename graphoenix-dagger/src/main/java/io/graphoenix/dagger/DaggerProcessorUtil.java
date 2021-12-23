package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

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
}
