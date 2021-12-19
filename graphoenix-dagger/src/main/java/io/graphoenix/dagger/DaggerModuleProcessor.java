package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import io.graphoenix.spi.annotation.dagger.GPXComponent;
import io.graphoenix.spi.aop.InterceptorBean;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                                                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(GPXComponent.class))
                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                                    .map(methodDeclaration ->
                                                            buildComponent(
                                                                    compilationUnit.getPackageDeclaration().orElseThrow(),
                                                                    classOrInterfaceDeclaration,
                                                                    methodDeclaration,
                                                                    compilationUnit.getImports().stream()
                                                                            .filter(importDeclaration ->
                                                                                    importDeclaration.getName().getIdentifier().equals(methodDeclaration.getType().asClassOrInterfaceType().getName().getIdentifier())
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

                                            classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .filter(bodyDeclaration -> !bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                                                    .map(BodyDeclaration::toMethodDeclaration)
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                                    .forEach(methodDeclaration ->
                                                            buildProxy(
                                                                    compilationUnit,
                                                                    classOrInterfaceDeclaration,
                                                                    methodDeclaration,
                                                                    getSourceByType(compilationUnit, methodDeclaration.getType().asClassOrInterfaceType())
                                                            )
                                                    );
                                        }
                                )
                );
    }

    protected List<MethodDeclaration> getInterceptorBeanMethodDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.getType().isClassOrInterfaceType())
                .collect(Collectors.toList());
    }

    protected Optional<MethodDeclaration> getInterceptorBeanMethodDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, AnnotationExpr annotationExpr) {
        return getInterceptorBeanMethodDeclarations(classOrInterfaceDeclaration).stream()
                .filter(methodDeclaration ->
                        methodDeclaration.getAnnotationByClass(InterceptorBean.class)
                                .flatMap(methodAnnotationExpr ->
                                        methodAnnotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                                .findFirst()
                                )
                                .filter(memberValuePair ->
                                        memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType().getNameAsString().equals(annotationExpr.getNameAsString())
                                )
                                .isPresent()
                )
                .findFirst();
    }

    protected Optional<MethodDeclaration> getModuleTypeMethodDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, SimpleName name) {
        return getInterceptorBeanMethodDeclarations(classOrInterfaceDeclaration).stream()
                .filter(MethodDeclaration::isMethodDeclaration)
                .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                .filter(methodDeclaration -> methodDeclaration.getType().asClassOrInterfaceType().getName().getIdentifier().equals(name.getIdentifier()))
                .findFirst();
    }


    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(type.getName().getIdentifier()))
                .map(NodeWithName::getNameAsString)
                .findFirst()
                .orElseGet(() -> compilationUnit.getPackageDeclaration()
                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(type.getNameAsString()))
                        .orElseGet(type::getNameAsString));
    }

    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceDeclaration declaration) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(declaration.getName().getIdentifier()))
                .map(NodeWithName::getNameAsString)
                .findFirst()
                .orElseGet(() -> compilationUnit.getPackageDeclaration()
                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(declaration.getNameAsString()))
                        .orElseGet(declaration::getNameAsString));
    }

    protected TypeElement getElementByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return processingEnv.getElementUtils().getTypeElement(getNameByType(compilationUnit, type));
    }

    protected String getSourceByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return trees.getPath(getElementByType(compilationUnit, type)).getCompilationUnit().toString();
    }

    protected void buildProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleDeclaration, MethodDeclaration methodDeclaration, String methodTypeSource) {

        javaParser.parse(methodTypeSource)
                .ifSuccessful(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .map(typeDeclaration -> (ClassOrInterfaceDeclaration) typeDeclaration)
                                .forEach(classOrInterfaceDeclaration -> {

                                            List<MethodDeclaration> interceptorBeanMethodDeclarations = classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(bodyDeclaration -> !bodyDeclaration.isConstructorDeclaration())
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                                                    .flatMap(moduleMethodDeclaration -> moduleMethodDeclaration.getAnnotations().stream()
                                                            .map(annotationExpr -> getInterceptorBeanMethodDeclaration(moduleDeclaration, annotationExpr))
                                                            .filter(Optional::isPresent)
                                                            .map(Optional::get)
                                                    )
                                                    .collect(Collectors.toList());

                                            ClassOrInterfaceDeclaration proxyDeclaration = new ClassOrInterfaceDeclaration()
                                                    .addExtendedType(classOrInterfaceDeclaration.getNameAsString())
                                                    .setName(classOrInterfaceDeclaration.getNameAsString() + "Proxy");


                                            CompilationUnit proxyCompilationUnit = new CompilationUnit()
                                                    .addImport(getNameByType(moduleCompilationUnit, classOrInterfaceDeclaration))
                                                    .addImport(Inject.class)
                                                    .addType(proxyDeclaration);

                                            interceptorBeanMethodDeclarations
                                                    .forEach(interceptorBeanMethodDeclaration -> {
                                                                proxyCompilationUnit.addImport(getNameByType(moduleCompilationUnit, interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()));
                                                                proxyDeclaration.addField(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE);
                                                            }
                                                    );

                                            classOrInterfaceDeclaration.getConstructors().forEach(
                                                    constructorDeclaration -> {

                                                        constructorDeclaration.getParameters()
                                                                .forEach(parameter -> proxyCompilationUnit.addImport(getNameByType(moduleCompilationUnit, parameter.getType().asClassOrInterfaceType())));

                                                        ConstructorDeclaration proxyConstructor = proxyDeclaration
                                                                .addConstructor(Modifier.Keyword.PUBLIC)
                                                                .addAnnotation(Inject.class)
                                                                .setParameters(constructorDeclaration.getParameters());

                                                        BlockStmt blockStmt = new BlockStmt()
                                                                .addStatement("super("
                                                                        .concat(constructorDeclaration.getParameters().stream()
                                                                                .map(NodeWithSimpleName::getNameAsString)
                                                                                .collect(Collectors.joining(","))
                                                                        )
                                                                        .concat(");")
                                                                );

                                                        interceptorBeanMethodDeclarations.forEach(
                                                                interceptorBeanMethodDeclaration -> {
                                                                    proxyConstructor.addParameter(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString());
                                                                    blockStmt.addStatement("this.".concat(interceptorBeanMethodDeclaration.getNameAsString()).concat("=").concat(interceptorBeanMethodDeclaration.getNameAsString()).concat(";"));
                                                                }
                                                        );

                                                        proxyConstructor.setBody(blockStmt);
                                                    }
                                            );

                                            classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .forEach(superMethodDeclaration -> {
                                                        List<MethodDeclaration> methodDeclarations = superMethodDeclaration.getAnnotations().stream()
                                                                .map(annotationExpr -> getInterceptorBeanMethodDeclaration(moduleDeclaration, annotationExpr))
                                                                .filter(Optional::isPresent)
                                                                .map(Optional::get)
                                                                .collect(Collectors.toList());

                                                        if (methodDeclarations.size() > 0) {
                                                            proxyDeclaration.addMethod(superMethodDeclaration.getNameAsString(), superMethodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                                                    .addAnnotation(Override.class);
                                                        }
                                                    });

                                            TypeDeclaration<?> componentType = proxyCompilationUnit.getType(0);

                                            try {
                                                Writer writer = processingEnv.getFiler().createSourceFile(componentType.getFullyQualifiedName().orElseGet(componentType::getNameAsString)).openWriter();
                                                writer.write(proxyCompilationUnit.toString());
                                                writer.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                )
                );
    }

    protected CompilationUnit buildComponent(PackageDeclaration packageDeclaration, ClassOrInterfaceDeclaration moduleDeclaration, MethodDeclaration methodDeclaration, ImportDeclaration importDeclaration) {
        ClassOrInterfaceType methodDeclarationType = methodDeclaration.getType().asClassOrInterfaceType();
        AnnotationExpr annotationExpr = methodDeclaration.getAnnotationByClass(GPXComponent.class).orElseThrow();

        String getMethodName = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("getMethodName"))
                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr().asString())
                .findFirst()
                .orElseGet(() -> getAnnotationDefaultValue(GPXComponent.class, "getMethodName", String.class).orElseThrow());

        String componentName = annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
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
