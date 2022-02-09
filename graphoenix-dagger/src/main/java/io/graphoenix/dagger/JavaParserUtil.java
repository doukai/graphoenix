package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum JavaParserUtil {
    JAVA_PARSER_UTIL;

    public Stream<ResolvedType> getMethodReturnType(MethodDeclaration methodDeclaration) {
        return methodDeclaration.findAll(ReturnStmt.class).stream()
                .map(ReturnStmt::getExpression)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Expression::calculateResolvedType);
    }

    public Stream<ResolvedReferenceType> getMethodReturnReferenceType(MethodDeclaration methodDeclaration) {
        return getMethodReturnType(methodDeclaration)
                .filter(ResolvedType::isReferenceType)
                .map(ResolvedType::asReferenceType);
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
