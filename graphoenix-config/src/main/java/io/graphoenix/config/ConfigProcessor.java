package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import io.graphoenix.inject.ComponentProxyProcessor;
import io.graphoenix.inject.ProcessorManager;
import io.graphoenix.inject.error.InjectionProcessException;
import io.vavr.control.Try;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.List;

import static io.graphoenix.inject.error.InjectionProcessErrorType.CONFIG_PROPERTIES_PREFIX_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.CONFIG_PROPERTY_NOT_EXIST;

@AutoService(ComponentProxyProcessor.class)
public class ConfigProcessor implements ComponentProxyProcessor {

    private ProcessorManager processorManager;

    private List<CompilationUnit> configPropertiesCompilationUnitLis;

    @Override
    public void init(ProcessorManager processorManager) {
        this.processorManager = processorManager;
    }

    @Override
    public void inProcess() {
        configPropertiesCompilationUnitLis = processorManager.getCompilationUnitListWithAnnotationClass(ConfigProperties.class);
    }

    @Override
    public void processComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        componentClassDeclaration.getFields().stream()
                .filter(fieldDeclaration -> fieldDeclaration.isAnnotationPresent(ConfigProperty.class))
                .filter(fieldDeclaration -> fieldDeclaration.hasModifier(Modifier.Keyword.PUBLIC) || fieldDeclaration.hasModifier(Modifier.Keyword.PROTECTED))
                .forEach(fieldDeclaration -> {
                            componentProxyCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class).addImport(Converter.class).addImport(processorManager.getQualifiedNameByType(fieldDeclaration.getCommonType()));
                            AnnotationExpr annotationExpr = fieldDeclaration.getAnnotationByClass(ConfigProperty.class).orElseThrow(() -> new InjectionProcessException(CONFIG_PROPERTY_NOT_EXIST.bind(fieldDeclaration.toString())));

                            StringLiteralExpr name = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                                    .findFirst()
                                    .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                    .orElseGet(() ->
                                            processorManager.getPublicClassOrInterfaceDeclaration(processorManager.getCompilationUnitByType(fieldDeclaration.getElementType()))
                                                    .getAnnotationByClass(ConfigProperties.class)
                                                    .flatMap(typeAnnotationExpr ->
                                                            typeAnnotationExpr
                                                                    .asNormalAnnotationExpr().getPairs().stream()
                                                                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("prefix"))
                                                                    .findFirst()
                                                                    .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                                    )
                                                    .orElseGet(() -> new StringLiteralExpr(Try.of(() -> (String) ConfigProperty.class.getMethod("name").getDefaultValue()).get()))
                                    );

                            StringLiteralExpr defaultValue = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("defaultValue"))
                                    .findFirst()
                                    .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                    .orElseGet(() -> new StringLiteralExpr(Try.of(() -> (String) ConfigProperty.class.getMethod("defaultValue").getDefaultValue()).get()));

                            componentProxyClassDeclaration.getConstructors()
                                    .forEach(constructorDeclaration ->
                                            fieldDeclaration.getVariables()
                                                    .forEach(variableDeclarator ->
                                                            constructorDeclaration.getBody()
                                                                    .addStatement(
                                                                            new AssignExpr()
                                                                                    .setTarget(variableDeclarator.getNameAsExpression())
                                                                                    .setValue(getConfigWithDefaultMethodCall(fieldDeclaration.getCommonType().asString(), name, defaultValue))
                                                                    )
                                                    )
                                    );
                        }
                );
    }

    @Override
    public void processModuleContext(CompilationUnit moduleCompilationUnit, BlockStmt staticInitializer) {
        configPropertiesCompilationUnitLis
                .forEach(compilationUnit -> {
                            moduleCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class);
                            ClassOrInterfaceDeclaration configClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);
                            String putClassQualifiedName = processorManager.getQualifiedNameByDeclaration(configClassDeclaration);

                            StringLiteralExpr propertyName = configClassDeclaration.getAnnotationByClass(ConfigProperties.class)
                                    .flatMap(annotationExpr ->
                                            annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("prefix"))
                                                    .findFirst()
                                                    .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                    )
                                    .orElseThrow(() -> new InjectionProcessException(CONFIG_PROPERTIES_PREFIX_NOT_EXIST.bind(processorManager.getQualifiedNameByDeclaration(configClassDeclaration))));


                            staticInitializer.addStatement(
                                    new MethodCallExpr()
                                            .setName("put")
                                            .addArgument(new ClassExpr().setType(putClassQualifiedName))
                                            .addArgument(
                                                    new LambdaExpr()
                                                            .setEnclosingParameters(true)
                                                            .setBody(
                                                                    new ExpressionStmt()
                                                                            .setExpression(
                                                                                    getConfigMethodCall(
                                                                                            configClassDeclaration.getFullyQualifiedName().orElseGet(configClassDeclaration::getNameAsString),
                                                                                            propertyName,
                                                                                            new StringLiteralExpr(Try.of(() -> (String) ConfigProperties.class.getMethod("prefix").getDefaultValue()).get())
                                                                                    )
                                                                            )
                                                            )
                                            )
                            );
                        }
                );
    }

    private MethodCallExpr getConfigMethodCall(String typeName, StringLiteralExpr propertyName, StringLiteralExpr defaultPropertyName) {
        return new MethodCallExpr()
                .setName("orElseGet")
                .addArgument(
                        new LambdaExpr()
                                .setEnclosingParameters(true)
                                .setBody(
                                        new ExpressionStmt(
                                                new MethodCallExpr()
                                                        .setName("orElse")
                                                        .addArgument(new ObjectCreationExpr().setType(typeName))
                                                        .setScope(
                                                                new MethodCallExpr().setName("getOptionalValue")
                                                                        .addArgument(defaultPropertyName)
                                                                        .addArgument(new ClassExpr().setType(typeName))
                                                                        .setScope(new MethodCallExpr().setName("getConfig").setScope(new NameExpr("ConfigProvider")))
                                                        )

                                        )
                                )
                )
                .setScope(
                        new MethodCallExpr().setName("getOptionalValue")
                                .addArgument(propertyName)
                                .addArgument(new ClassExpr().setType(typeName))
                                .setScope(new MethodCallExpr().setName("getConfig").setScope(new NameExpr("ConfigProvider")))
                );
    }

    private MethodCallExpr getConfigWithDefaultMethodCall(String typeName, StringLiteralExpr propertyName, StringLiteralExpr defaultValue) {
        return new MethodCallExpr()
                .setName("orElseGet")
                .addArgument(
                        new LambdaExpr()
                                .setEnclosingParameters(true)
                                .setBody(
                                        new ExpressionStmt(
                                                new MethodCallExpr()
                                                        .setName("orElse")
                                                        .addArgument(new ObjectCreationExpr().setType(typeName))
                                                        .setScope(
                                                                new MethodCallExpr()
                                                                        .setName("map")
                                                                        .addArgument(
                                                                                new LambdaExpr()
                                                                                        .addParameter(
                                                                                                new ClassOrInterfaceType()
                                                                                                        .setName(Converter.class.getSimpleName())
                                                                                                        .setTypeArguments(new ClassOrInterfaceType().setName(typeName)),
                                                                                                "converter"
                                                                                        )
                                                                                        .setEnclosingParameters(true)
                                                                                        .setBody(
                                                                                                new ExpressionStmt(
                                                                                                        new MethodCallExpr()
                                                                                                                .setName("convert")
                                                                                                                .addArgument(defaultValue)
                                                                                                                .setScope(new NameExpr("converter"))
                                                                                                )
                                                                                        )
                                                                        )
                                                                        .setScope(
                                                                                new MethodCallExpr()
                                                                                        .setName("getConverter")
                                                                                        .addArgument(new ClassExpr().setType(typeName))
                                                                                        .setScope(new MethodCallExpr().setName("getConfig").setScope(new NameExpr("ConfigProvider")))
                                                                        )
                                                        )
                                        )
                                )
                )
                .setScope(
                        new MethodCallExpr().setName("getOptionalValue")
                                .addArgument(propertyName)
                                .addArgument(new ClassExpr().setType(typeName))
                                .setScope(new MethodCallExpr().setName("getConfig").setScope(new NameExpr("ConfigProvider")))
                );
    }
}
