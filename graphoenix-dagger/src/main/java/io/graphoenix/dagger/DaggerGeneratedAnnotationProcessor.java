package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("dagger.Component")
@AutoService(Processor.class)
public class DaggerGeneratedAnnotationProcessor extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                ServiceLoader<DaggerExpansionProcessor> expansionProcessors = ServiceLoader.load(DaggerExpansionProcessor.class, DaggerGeneratedAnnotationProcessor.class.getClassLoader());
                Iterator<DaggerExpansionProcessor> processorIterator = expansionProcessors.iterator();
                String sourceCode = trees.getPath(bundleClassElement).getCompilationUnit().toString();
                Filer filer = this.processingEnv.getFiler();
                final Elements elementUtils = this.processingEnv.getElementUtils();
                PackageElement packageElement = elementUtils.getPackageOf(bundleClassElement);
                while (processorIterator.hasNext()) {
                    sourceCode = processorIterator.next().process(sourceCode, filer);
                }
                buildExpansionComponent(sourceCode, filer, packageElement);
            }
        }
        return false;
    }

    protected void buildExpansionComponent(String sourceCode, Filer filer, PackageElement packageElement) {
        JavaParser javaParser = new JavaParser();
        javaParser.parse(sourceCode).ifSuccessful(
                compilationUnit -> {
                    compilationUnit.getTypes().stream().filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                            .map(typeDeclaration -> (ClassOrInterfaceDeclaration) typeDeclaration)
                            .forEach(
                                    classOrInterfaceDeclaration -> {
                                        SimpleName declarationName = classOrInterfaceDeclaration.getName();
                                        NodeList<AnnotationExpr> annotations = classOrInterfaceDeclaration.getAnnotations();
                                        compilationUnit.getImports().stream()
                                                .filter(importDeclaration ->
                                                        annotations.stream()
                                                                .map(annotationExpr -> annotationExpr.getName().getIdentifier())
                                                                .collect(Collectors.toList())
                                                                .contains(importDeclaration.getName().getIdentifier())
                                                )
                                                .collect(Collectors.toList())
                                                .forEach(compilationUnit::remove);
                                        annotations.clear();

                                        classOrInterfaceDeclaration
                                                .addImplementedType(new ClassOrInterfaceType().setName(declarationName))
                                                .setInterface(false)
                                                .setPublic(true)
                                                .setFinal(true)
                                                .setName("GPX".concat(declarationName.asString()))
                                                .getMembers()
                                                .stream().filter(BodyDeclaration::isMethodDeclaration)
                                                .map(BodyDeclaration::toMethodDeclaration)
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                                                .forEach(methodDeclaration ->
                                                        methodDeclaration
                                                                .addAnnotation(Override.class)
                                                                .setPublic(true)
                                                                .createBody()
                                                                .addStatement(
                                                                        new ReturnStmt().setExpression(
                                                                                new MethodCallExpr().setName(
                                                                                        "Dagger".concat(declarationName.asString())
                                                                                                .concat(".create().")
                                                                                                .concat(methodDeclaration.getNameAsString())
                                                                                )
                                                                        )
                                                                )
                                                );
                                        try {
                                            Writer writer = filer.createSourceFile(classOrInterfaceDeclaration.getFullyQualifiedName().orElseGet(classOrInterfaceDeclaration::getNameAsString), packageElement).openWriter();
                                            writer.write(compilationUnit.toString());
                                            writer.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                            );
                }
        );
    }
}
