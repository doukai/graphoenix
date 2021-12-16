package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import io.graphoenix.spi.annotation.dagger.AutoComponent;

import javax.annotation.processing.*;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.Set;

@SupportedAnnotationTypes("dagger.Module")
@AutoService(Processor.class)
public class DaggerModuleProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                Filer filer = this.processingEnv.getFiler();
                buildComponents(trees.getPath(bundleClassElement).getCompilationUnit().toString(), filer);
            }
        }
        return false;
    }

    protected void buildComponents(String sourceCode, Filer filer) {
        javaParser.parse(sourceCode)
                .ifSuccessful(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .map(typeDeclaration -> (ClassOrInterfaceDeclaration) typeDeclaration)
                                .forEach(classOrInterfaceDeclaration -> {
                                    classOrInterfaceDeclaration.getMembers().stream()
                                            .filter(BodyDeclaration::isMethodDeclaration)
                                            .map(BodyDeclaration::toMethodDeclaration)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(AutoComponent.class))
                                            .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                            .map(methodDeclaration ->
                                                    buildComponent(
                                                            compilationUnit.getPackageDeclaration().orElseThrow(),
                                                            classOrInterfaceDeclaration,
                                                            methodDeclaration,
                                                            compilationUnit.getImports().stream()
                                                                    .filter(importDeclaration ->
                                                                            importDeclaration.getName().getIdentifier().equals(((ClassOrInterfaceType) methodDeclaration.getType()).getName().getIdentifier())
                                                                    ).findFirst().orElseThrow()
                                                    )
                                            )
                                            .forEach(ComponentCompilationUnit -> {
                                                        TypeDeclaration<?> componentType = ComponentCompilationUnit.getType(0);
                                                        try {
                                                            Writer writer = filer.createSourceFile(componentType.getFullyQualifiedName().orElseGet(componentType::getNameAsString)).openWriter();
                                                            writer.write(ComponentCompilationUnit.toString());
                                                            writer.close();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                            );
                                })
                );
    }

    protected CompilationUnit buildComponent(PackageDeclaration packageDeclaration, ClassOrInterfaceDeclaration moduleDeclaration, MethodDeclaration methodDeclaration, ImportDeclaration importDeclaration) {
        ClassOrInterfaceType methodDeclarationType = (ClassOrInterfaceType) methodDeclaration.getType();
        AnnotationExpr annotationExpr = methodDeclaration.getAnnotationByClass(AutoComponent.class).orElseThrow();

        String getMethodName = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr().asString())
                .findFirst()
                .orElseGet(() -> getAnnotationDefaultValue(AutoComponent.class, "value", String.class).orElseThrow());

        String componentName = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr().asString())
                .findFirst()
                .orElseGet(() -> methodDeclarationType.getNameAsString().concat("Component"));

        String packageName = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("packageName"))
                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr().asString())
                .findFirst()
                .orElseGet(packageDeclaration::getNameAsString);

        CompilationUnit compilationUnit = new CompilationUnit()
                .setPackageDeclaration(packageName)
                .addImport(importDeclaration)
                .addImport(moduleDeclaration.getFullyQualifiedName().orElseGet(methodDeclaration::getNameAsString))
                .addImport(Singleton.class)
                .addImport(Component.class);
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentName)
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", new ClassExpr().setType(moduleDeclaration.getNameAsString())).setName(Component.class.getSimpleName()));

        classOrInterfaceDeclaration.addMethod(getMethodName).setType(methodDeclarationType).removeBody();

        compilationUnit.addType(classOrInterfaceDeclaration);
        return compilationUnit;
    }

    protected <A, T> Optional<T> getAnnotationDefaultValue(Class<A> annotationClass, String name, Class<T> returnClass) {
        try {
            return Optional.of(returnClass.cast(annotationClass.getMethod(name).getDefaultValue()));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
