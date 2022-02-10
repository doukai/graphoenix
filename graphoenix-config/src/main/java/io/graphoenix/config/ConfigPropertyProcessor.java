package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.graphoenix.inject.DaggerProxyProcessor;
import io.graphoenix.inject.ProcessorManager;
import io.vavr.control.Try;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigPropertyProcessor implements DaggerProxyProcessor {

    private ProcessorManager processorManager;

    @Override
    public void init(ProcessorManager processorManager) {
        this.processorManager = processorManager;
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
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleClassDeclaration,
                                 List<CompilationUnit> componentProxyCompilationUnits,
                                 CompilationUnit moduleProxyCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        if (moduleClassDeclaration.isInterface()) {
            return;
        }

        moduleProxyClassDeclaration.addField(Config.class, "config", Modifier.Keyword.PRIVATE).getVariable(0).setInitializer(new MethodCallExpr().setName("getConfig").setScope(new NameExpr("ConfigProvider")));

        List<FieldDeclaration> configPropertyFieldDeclarations = getConfigPropertyFieldDeclarations(moduleClassDeclaration);

        configPropertyFieldDeclarations
                .forEach(fieldDeclaration ->
                        moduleProxyClassDeclaration.addField(fieldDeclaration.getElementType(), fieldDeclaration.getVariable(0).getNameAsString(), fieldDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                .getVariable(0)
                                .setInitializer(
                                        new MethodCallExpr()
                                                .setName("orElseGet")
                                                .addArgument(
                                                        new LambdaExpr()
                                                                .setEnclosingParameters(true)
                                                                .setBody(
                                                                        new ExpressionStmt(
                                                                                new MethodCallExpr()
                                                                                        .setName("orElse")
                                                                                        .addArgument(new ObjectCreationExpr().setType(fieldDeclaration.getElementType().asClassOrInterfaceType()))
                                                                                        .setScope(
                                                                                                new MethodCallExpr().setName("getOptionalValue")
                                                                                                        .addArgument(fieldDeclaration
                                                                                                                .getAnnotationByClass(ConfigProperty.class)
                                                                                                                .map(annotationExpr ->
                                                                                                                        annotationExpr
                                                                                                                                .asNormalAnnotationExpr().getPairs().stream()
                                                                                                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("defaultValue"))
                                                                                                                                .findFirst()
                                                                                                                                .map(MemberValuePair::getValue)
                                                                                                                                .orElseGet(() -> new StringLiteralExpr(Try.of(() -> (String) ConfigProperty.class.getMethod("defaultValue").getDefaultValue()).get()))
                                                                                                                )
                                                                                                                .orElseThrow()
                                                                                                        )
                                                                                                        .addArgument(new ClassExpr().setType(fieldDeclaration.getElementType()))
                                                                                                        .setScope(new NameExpr("config"))))
                                                                )
                                                )
                                                .setScope(
                                                        new MethodCallExpr().setName("getOptionalValue")
                                                                .addArgument(fieldDeclaration
                                                                        .getAnnotationByClass(ConfigProperty.class)
                                                                        .orElseThrow()
                                                                        .asNormalAnnotationExpr().getPairs().stream()
                                                                        .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                                                                        .findFirst()
                                                                        .orElseGet(() -> processorManager.getCompilationUnitByClassOrInterfaceType(fieldDeclaration.getElementType().asClassOrInterfaceType())
                                                                                .map(processorManager::getPublicClassOrInterfaceDeclaration)
                                                                                .filter(Optional::isPresent)
                                                                                .map(Optional::get)
                                                                                .map(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getAnnotationByClass(ConfigProperties.class))
                                                                                .filter(Optional::isPresent)
                                                                                .map(Optional::get)
                                                                                .map(annotationExpr ->
                                                                                        annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("prefix"))
                                                                                                .findFirst()
                                                                                                .orElseThrow()
                                                                                )
                                                                                .orElseThrow())
                                                                        .getValue()
                                                                )
                                                                .addArgument(new ClassExpr().setType(fieldDeclaration.getElementType()))
                                                                .setScope(new NameExpr("config"))
                                                )
                                )
                );

        moduleProxyCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class);
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
        return ConfigProperty.class;
    }

    protected List<FieldDeclaration> getConfigPropertyFieldDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(ConfigProperty.class))
                .map(bodyDeclaration -> (FieldDeclaration) bodyDeclaration)
                .collect(Collectors.toList());
    }
}
