package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import dagger.Provides;
import io.graphoenix.inject.ComponentProxyProcessor;
import io.graphoenix.inject.ProcessorManager;
import io.vavr.control.Try;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.util.List;

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

    }

    @Override
    public void processModule(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {
        configPropertiesCompilationUnitLis
                .forEach(compilationUnit -> {
                            moduleCompilationUnit.addImport(Config.class).addImport(ConfigProvider.class);
                            ClassOrInterfaceDeclaration configClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();

                            MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, configClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                                    .addAnnotation(Provides.class)
                                    .addAnnotation(Singleton.class)
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
}
