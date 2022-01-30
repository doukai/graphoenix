package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum DaggerProcessorUtil {
    DAGGER_PROCESSOR_UTIL;

    public Stream<ClassOrInterfaceType> getMethodReturnType(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody()
                .map(blockStmt -> blockStmt.getStatements().stream()
                        .filter(Statement::isReturnStmt)
                        .map(Statement::asReturnStmt)
                        .map(ReturnStmt::getExpression)
                        .map(Optional::orElseThrow)
                        .filter(Expression::isObjectCreationExpr)
                        .map(Expression::asObjectCreationExpr)
                        .map(ObjectCreationExpr::getType)
                )
                .orElseGet(() -> Stream.of(methodDeclaration.getType().asClassOrInterfaceType()));
    }

    public boolean hasSameParameters(NodeList<Parameter> target, NodeList<Parameter> source) {
        if (target.size() != source.size()) {
            return false;
        }
        return IntStream.range(0, target.size())
                .mapToObj(index -> target.get(index).getType().asString().equals(source.get(index).getType().asString()))
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
