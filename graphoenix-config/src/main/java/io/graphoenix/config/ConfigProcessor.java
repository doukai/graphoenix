package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import dagger.Component;
import dagger.Provides;
import io.graphoenix.inject.ComponentProxyProcessor;
import io.graphoenix.inject.ProcessorManager;
import io.vavr.control.Try;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.List;
import java.util.stream.Collectors;

@AutoService(ComponentProxyProcessor.class)
public class ConfigProcessor implements ComponentProxyProcessor {

    private ProcessorManager processorManager;

    private List<CompilationUnit> configPropertiesCompilationUnitLis;

    private List<CompilationUnit> configPropertiesComponentCompilationUnitLis;

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
                            AnnotationExpr annotationExpr = fieldDeclaration.getAnnotationByClass(ConfigProperty.class).orElseThrow();

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
    public void processModule(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {
        configPropertiesCompilationUnitLis
                .forEach(compilationUnit -> {
                            moduleCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class);
                            ClassOrInterfaceDeclaration configClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);

                            MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, configClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                                    .addAnnotation(Provides.class)
                                    .addAnnotation(javax.inject.Singleton.class)
                                    .setType(configClassDeclaration.getNameAsString());

                            StringLiteralExpr propertyName = configClassDeclaration.getAnnotationByClass(ConfigProperties.class)
                                    .flatMap(annotationExpr ->
                                            annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("prefix"))
                                                    .findFirst()
                                                    .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                    )
                                    .orElseThrow();

                            methodDeclaration.createBody()
                                    .addStatement(
                                            new ReturnStmt(
                                                    getConfigMethodCall(
                                                            configClassDeclaration.getNameAsString(),
                                                            propertyName,
                                                            new StringLiteralExpr(Try.of(() -> (String) ConfigProperties.class.getMethod("prefix").getDefaultValue()).get())
                                                    )
                                            )
                                    );
                            processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, configClassDeclaration);
                        }
                );

        configPropertiesComponentCompilationUnitLis = configPropertiesCompilationUnitLis.stream()
                .map(compilationUnit -> buildConfigComponent(compilationUnit, moduleClassDeclaration))
                .collect(Collectors.toList());

        configPropertiesComponentCompilationUnitLis.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));
    }

    @Override
    public void processComponentModule(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {
        moduleClassDeclaration.getFields().stream()
                .filter(fieldDeclaration -> fieldDeclaration.isAnnotationPresent(ConfigProperty.class))
                .filter(fieldDeclaration -> fieldDeclaration.hasModifier(Modifier.Keyword.PUBLIC) || fieldDeclaration.hasModifier(Modifier.Keyword.PROTECTED))
                .forEach(fieldDeclaration -> {
                            moduleCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class).addImport(Converter.class).addImport(processorManager.getQualifiedNameByType(fieldDeclaration.getCommonType()));
                            AnnotationExpr annotationExpr = fieldDeclaration.getAnnotationByClass(ConfigProperty.class).orElseThrow();

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

                            moduleClassDeclaration.getConstructors()
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
    public void processModuleContext(CompilationUnit moduleContextCompilationUnit, BlockStmt moduleContextStaticInitializer) {
        configPropertiesComponentCompilationUnitLis
                .forEach(compilationUnit -> {
                            ClassOrInterfaceDeclaration configPropertiesComponentClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);
                            String daggerClassName = "Dagger".concat(configPropertiesComponentClassDeclaration.getNameAsString());
                            String daggerVariableName = "dagger".concat(configPropertiesComponentClassDeclaration.getNameAsString());

                            ClassOrInterfaceType componentType = configPropertiesComponentClassDeclaration.getMembers().stream()
                                    .filter(BodyDeclaration::isMethodDeclaration)
                                    .map(BodyDeclaration::asMethodDeclaration)
                                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals("get"))
                                    .map(MethodDeclaration::getType)
                                    .filter(Type::isClassOrInterfaceType)
                                    .map(Type::asClassOrInterfaceType)
                                    .findFirst()
                                    .orElseThrow();

                            moduleContextStaticInitializer.addStatement(new VariableDeclarationExpr()
                                    .addVariable(
                                            new VariableDeclarator()
                                                    .setType(configPropertiesComponentClassDeclaration.getNameAsString())
                                                    .setName(daggerVariableName)
                                                    .setInitializer(
                                                            new MethodCallExpr()
                                                                    .setName("create")
                                                                    .setScope(new NameExpr().setName(daggerClassName))
                                                    )
                                    )
                            );

                            moduleContextStaticInitializer.addStatement(
                                    new MethodCallExpr()
                                            .setName("put")
                                            .addArgument(new ClassExpr().setType(componentType.getNameAsString()))
                                            .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
                            );

                            moduleContextCompilationUnit.addImport(processorManager.getQualifiedNameByType(componentType));
                            compilationUnit.getPackageDeclaration()
                                    .ifPresent(packageDeclaration -> {
                                                moduleContextCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(configPropertiesComponentClassDeclaration.getNameAsString()));
                                                moduleContextCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(daggerClassName));
                                            }
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

    private CompilationUnit buildConfigComponent(CompilationUnit configCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ClassOrInterfaceDeclaration configClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(configCompilationUnit);
        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(configClassDeclaration.getNameAsString() + "_Component")
                .addAnnotation(javax.inject.Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        componentProxyComponentInterfaceDeclaration
                .addMethod("get")
                .setType(configClassDeclaration.getNameAsString())
                .removeBody();

        CompilationUnit componentProxyComponentCompilationUnit = new CompilationUnit()
                .addType(componentProxyComponentInterfaceDeclaration)
                .addImport(Component.class);

        configCompilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> componentProxyComponentCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));

        componentProxyComponentCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(moduleClassDeclaration));

        processorManager.importAllClassOrInterfaceType(componentProxyComponentInterfaceDeclaration, configClassDeclaration);
        processorManager.importAllClassOrInterfaceType(componentProxyComponentInterfaceDeclaration, moduleClassDeclaration);
        return componentProxyComponentCompilationUnit;
    }
}
