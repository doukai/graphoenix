package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;

import static javax.lang.model.element.ElementKind.METHOD;

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
                                            .map(methodDeclaration -> buildComponent(classOrInterfaceDeclaration, methodDeclaration));


                                })
                );
    }

    protected CompilationUnit buildComponent(ClassOrInterfaceDeclaration moduleDeclaration, MethodDeclaration methodDeclaration) {
        ClassOrInterfaceType methodDeclarationType = (ClassOrInterfaceType) methodDeclaration.getType();
        AnnotationExpr annotationExpr = methodDeclaration.getAnnotationByClass(AutoComponent.class).orElseThrow();

        CompilationUnit compilationUnit = new CompilationUnit();
        compilationUnit.addImport(methodDeclarationType.getNameWithScope());
        compilationUnit.addImport(moduleDeclaration.getFullyQualifiedName().orElseGet(methodDeclaration::getNameAsString));
        ClassOrInterfaceType classOrInterfaceType = new ClassOrInterfaceType()
                .setName(methodDeclarationType.getNameAsString().concat("Factory"))
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", new ClassExpr().setType(moduleDeclaration.getNameAsString())));

        return null;
    }
}
