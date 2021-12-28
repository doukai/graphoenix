package io.graphoenix.patterns;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.Optional;

public enum PatternsUtil {
    PATTERNS_UTIL;

    public Optional<String> getBuilderVariableName(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody().orElseThrow()
                .getStatements().stream()
                .filter(Statement::isExpressionStmt)
                .map(statement -> statement.asExpressionStmt().getExpression())
                .filter(Expression::isVariableDeclarationExpr)
                .map(expression -> expression.asVariableDeclarationExpr().getVariable(0))
                .filter(variableDeclarator -> variableDeclarator.getType().isClassOrInterfaceType())
                .filter(variableDeclarator -> variableDeclarator.getType().asClassOrInterfaceType().getNameAsString().equals("ChainsBeanBuilder"))
                .map(NodeWithSimpleName::getNameAsString)
                .findFirst();
    }

    public Optional<ClassOrInterfaceType> getBuilderReturnType(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody().orElseThrow()
                .getStatements().stream()
                .filter(Statement::isReturnStmt)
                .map(Statement::asReturnStmt)
                .filter(returnStmt -> returnStmt.getExpression().isPresent())
                .map(returnStmt -> returnStmt.getExpression().get())
                .filter(Expression::isMethodCallExpr)
                .map(Expression::asMethodCallExpr)
                .filter(methodCallExpr -> methodCallExpr.getArgument(0).isClassExpr())
                .map(methodCallExpr -> methodCallExpr.getArgument(0).asClassExpr())
                .map(ClassExpr::getType)
                .filter(Type::isClassOrInterfaceType)
                .map(Type::asClassOrInterfaceType)
                .findFirst();
    }
}
