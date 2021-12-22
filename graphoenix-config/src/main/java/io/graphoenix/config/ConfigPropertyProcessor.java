package io.graphoenix.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.stream.Collectors;

@AutoService(DaggerProxyProcessor.class)
public class ConfigPropertyProcessor implements DaggerProxyProcessor {

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
                                 ClassOrInterfaceDeclaration moduleCLassDeclaration,
                                 List<CompilationUnit> componentProxyCompilationUnits,
                                 CompilationUnit moduleProxyCompilationUnit,
                                 ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {
        moduleProxyClassDeclaration.addField(Config.class, "config", Modifier.Keyword.PRIVATE).getVariable(0).setInitializer("ConfigProvider.getConfig();");

        List<FieldDeclaration> configPropertyFieldDeclarations = getConfigPropertyFieldDeclarations(moduleCLassDeclaration);

        configPropertyFieldDeclarations
                .forEach(fieldDeclaration ->
                        moduleProxyClassDeclaration.addField(fieldDeclaration.getElementType(), fieldDeclaration.getVariable(0).getNameAsString(), fieldDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                .getVariable(0)
                                .setInitializer(
                                        "config.getValue("
                                                .concat(
                                                        fieldDeclaration.getAnnotationByClass(ConfigProperty.class)
                                                                .orElseThrow()
                                                                .asNormalAnnotationExpr().getPairs().stream()
                                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                                                                .findFirst()
                                                                .orElseThrow()
                                                                .getValue().toString()
                                                )
                                                .concat(",")
                                                .concat(fieldDeclaration.getElementType().asClassOrInterfaceType().getNameAsString())
                                                .concat(".class);"))
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

    protected List<FieldDeclaration> getConfigPropertyFieldDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(ConfigProperty.class))
                .map(bodyDeclaration -> (FieldDeclaration) bodyDeclaration)
                .collect(Collectors.toList());
    }
}
