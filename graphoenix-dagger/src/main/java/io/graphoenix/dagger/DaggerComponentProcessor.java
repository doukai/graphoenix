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
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("dagger.Component")
@AutoService(Processor.class)
public class DaggerComponentProcessor extends AbstractProcessor {

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
                buildExpansionComponent(trees.getPath(bundleClassElement).getCompilationUnit().toString(), filer);
            }
        }
        return false;
    }

    protected void buildExpansionComponent(String sourceCode, Filer filer) {
        javaParser.parse(sourceCode)
                .ifSuccessful(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
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
                                                    .stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
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
                                                ServiceLoader<DaggerExpansionProcessor> expansionProcessors = ServiceLoader.load(DaggerExpansionProcessor.class, DaggerComponentProcessor.class.getClassLoader());
                                                expansionProcessors.forEach(daggerExpansionProcessor -> daggerExpansionProcessor.process(compilationUnit, filer));
                                                Writer writer = filer.createSourceFile(classOrInterfaceDeclaration.getFullyQualifiedName().orElseGet(classOrInterfaceDeclaration::getNameAsString)).openWriter();
                                                writer.write(compilationUnit.toString());
                                                writer.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                )
                );
    }
}
