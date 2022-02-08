package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum JavaParserUtil {
    JAVA_PARSER_UTIL;

    public Stream<String> getMethodReturnType(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody()
                .map(blockStmt -> blockStmt.getStatements().stream()
                        .filter(Statement::isReturnStmt)
                        .map(Statement::asReturnStmt)
                        .map(ReturnStmt::getExpression)
                        .map(Optional::orElseThrow)
                        .filter(Expression::isObjectCreationExpr)
                        .map(Expression::asObjectCreationExpr)
                        .map(ObjectCreationExpr::getType)
                        .map(ClassOrInterfaceType::getNameAsString)
                )
                .orElseGet(() -> Stream.of(methodDeclaration.getType().asString()));
    }

    public String getMethodCallExprType(CompilationUnit compilationUnit, MethodCallExpr methodCallExpr) {

        if (methodCallExpr.getScope().isEmpty() || methodCallExpr.getScope().get().isThisExpr()) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();
            classOrInterfaceDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodCallExpr.getNameAsString()))
                    .map(methodDeclaration -> methodDeclaration.getType().asClassOrInterfaceType())
                    .map(Type::asString)
                    .findFirst()
                    .

        } else if (methodCallExpr.getScope().get().isSuperExpr()) {

        } else {

        }

    }

    public String getArgumentTypeNameFromMethod(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, Expression argument) {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();
        if (argument.isNameExpr()) {
            return methodDeclaration.getParameters().stream()
                    .filter(parameter -> parameter.getNameAsString().equals(argument.asNameExpr().getNameAsString()))
                    .map(Parameter::getType)
                    .map(Type::asString)
                    .findFirst()
                    .orElseGet(() ->
                            methodDeclaration.getBody().orElseThrow().getStatements().stream()
                                    .filter(Statement::isExpressionStmt)
                                    .map(statement -> statement.asExpressionStmt().getExpression())
                                    .filter(Expression::isVariableDeclarationExpr)
                                    .flatMap(expression -> expression.asVariableDeclarationExpr().getVariables().stream())
                                    .filter(variableDeclarator -> variableDeclarator.getNameAsString().equals(argument.asNameExpr().getNameAsString()))
                                    .map(VariableDeclarator::getType)
                                    .map(Type::asString)
                                    .findFirst()
                                    .orElseGet(() ->
                                            classOrInterfaceDeclaration.getFields().stream()
                                                    .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                                                    .filter(variableDeclarator -> variableDeclarator.getNameAsString().equals(argument.asNameExpr().getNameAsString()))
                                                    .map(VariableDeclarator::getType)
                                                    .map(Type::asString)
                                                    .findFirst()
                                                    .orElseThrow()
                                    )
                    );
        } else if (argument.isMethodCallExpr()) {
            return getMethodCallExprType(compilationUnit, argument.asMethodCallExpr());
        } else if (argument.isObjectCreationExpr()) {
            return argument.asObjectCreationExpr().getType().asString();
        } else if (argument.isCastExpr()) {
            return argument.asCastExpr().getType().asString();
        } else if (argument.isArrayCreationExpr()) {
            return argument.asArrayCreationExpr().createdType().asString();
        } else if (argument.isThisExpr()) {
            return classOrInterfaceDeclaration.getNameAsString();
        } else if (argument.isSuperExpr()) {
            return classOrInterfaceDeclaration.getExtendedTypes(0).getNameAsString();
        }
        throw new RuntimeException();
    }

    public boolean hasSameParameters(NodeList<Parameter> target, NodeList<Parameter> source) {
        if (target.size() != source.size()) {
            return false;
        }
        return IntStream.range(0, target.size())
                .mapToObj(index -> target.get(index).getType().asString().equals(source.get(index).getType().asString()))
                .reduce(true, (left, right) -> left && right);
    }

    public boolean hasSameParameters(List<Type> target, List<Type> source) {
        if (target.size() != source.size()) {
            return false;
        }
        return IntStream.range(0, target.size())
                .mapToObj(index -> target.get(index).asString().equals(source.get(index).asString()))
                .reduce(true, (left, right) -> left && right);
    }

    public Optional<ClassOrInterfaceDeclaration> getPublicClassOrInterfaceDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst();
    }

    public Optional<AnnotationDeclaration> getPublicAnnotationDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isAnnotationDeclaration)
                .map(BodyDeclaration::asAnnotationDeclaration)
                .findFirst();
    }
}
