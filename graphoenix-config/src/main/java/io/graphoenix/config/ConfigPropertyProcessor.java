package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.dagger.ProcessorTools;
import io.vavr.control.Try;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@AutoService(DaggerProxyProcessor.class)
public class ConfigPropertyProcessor implements DaggerProxyProcessor {

    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;

    @Override
    public void init(ProcessorTools processorTools) {

        this.getCompilationUnitByClassOrInterfaceType = processorTools.getGetCompilationUnitByClassOrInterfaceType();

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
        return Optional.empty();
    }

    @Override
    public void prepareModuleProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

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
                                                                        .orElseGet(() -> getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, fieldDeclaration.getElementType().asClassOrInterfaceType())
                                                                                .map(DAGGER_PROCESSOR_UTIL::getPublicClassOrInterfaceDeclaration)
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
