package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import dagger.Module;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.aop.InvocationContext;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("dagger.Module")
@AutoService(Processor.class)
public class DaggerModuleProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;
    private Filer filer;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
        this.filer = this.processingEnv.getFiler();
        this.elements = this.processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                buildComponents(trees.getPath(bundleClassElement).getCompilationUnit().toString());
            }
        }
        return false;
    }

    protected void buildComponents(String sourceCode) {
        javaParser.parse(sourceCode)
                .ifSuccessful(compilationUnit ->
                                compilationUnit.getTypes().stream()
                                        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                        .map(typeDeclaration -> (ClassOrInterfaceDeclaration) typeDeclaration)
                                        .forEach(classOrInterfaceDeclaration -> {
//                                            classOrInterfaceDeclaration.getMembers().stream()
//                                                    .filter(BodyDeclaration::isMethodDeclaration)
//                                                    .map(BodyDeclaration::toMethodDeclaration)
//                                                    .filter(Optional::isPresent)
//                                                    .map(Optional::get)
//                                                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(GPXComponent.class))
//                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
//                                                    .map(methodDeclaration ->
//                                                            buildComponent(
//                                                                    compilationUnit.getPackageDeclaration().orElseThrow(),
//                                                                    classOrInterfaceDeclaration,
//                                                                    methodDeclaration,
//                                                                    compilationUnit.getImports().stream()
//                                                                            .filter(importDeclaration ->
//                                                                                    importDeclaration.getName().getIdentifier().equals(methodDeclaration.getType().asClassOrInterfaceType().getName().getIdentifier())
//                                                                            ).findFirst().orElseThrow()
//                                                            )
//                                                    )
//                                                    .forEach(this::writeToFiler);

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

    protected String getNameByType(CompilationUnit compilationUnit, Name name) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
                .map(NodeWithName::getNameAsString)
                .findFirst()
                .orElseGet(() -> compilationUnit.getImports().stream()
                        .filter(ImportDeclaration::isAsterisk)
                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
                        .map(importDeclaration -> importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                        .findFirst()
                        .orElseGet(() -> compilationUnit.getPackageDeclaration()
                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                                .orElseGet(name::asString))
                );
    }

    protected boolean isStaticImport(CompilationUnit compilationUnit, Name name) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
                .map(ImportDeclaration::isStatic)
                .findFirst()
                .orElseGet(() -> compilationUnit.getImports().stream()
                        .filter(ImportDeclaration::isAsterisk)
                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
                        .map(ImportDeclaration::isStatic)
                        .findFirst()
                        .orElse(false)
                );
    }

    protected String getNameByType(CompilationUnit compilationUnit, SimpleName name) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
                .map(NodeWithName::getNameAsString)
                .findFirst()
                .orElseGet(() -> compilationUnit.getImports().stream()
                        .filter(ImportDeclaration::isAsterisk)
                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
                        .map(importDeclaration -> importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                        .findFirst()
                        .orElseGet(() -> compilationUnit.getPackageDeclaration()
                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                                .orElseGet(name::asString))
                );
    }

    protected boolean isStaticImport(CompilationUnit compilationUnit, SimpleName name) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
                .map(ImportDeclaration::isStatic)
                .findFirst()
                .orElseGet(() -> compilationUnit.getImports().stream()
                        .filter(ImportDeclaration::isAsterisk)
                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
                        .map(ImportDeclaration::isStatic)
                        .findFirst()
                        .orElse(false)
                );
    }

    protected boolean classExist(String className) {
        return elements.getTypeElement(className) != null;
    }

    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return getNameByType(compilationUnit, type.getName());
    }

    protected boolean isStaticImport(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return isStaticImport(compilationUnit, type.getName());
    }

    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceDeclaration declaration) {
        return getNameByType(compilationUnit, declaration.getName());
    }

    protected boolean isStaticImport(CompilationUnit compilationUnit, ClassOrInterfaceDeclaration declaration) {
        return isStaticImport(compilationUnit, declaration.getName());
    }

    protected TypeElement getElementByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return elements.getTypeElement(getNameByType(compilationUnit, type));
    }

    protected String getSourceByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return trees.getPath(getElementByType(compilationUnit, type)).getCompilationUnit().toString();
    }

    protected void buildProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleDeclaration, MethodDeclaration methodDeclaration, String methodTypeSource) {

        javaParser.parse(methodTypeSource)
                .ifSuccessful(compilationUnit -> {

                            List<CompilationUnit> proxyCompilationUnits = compilationUnit.getTypes().stream()
                                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                    .map(typeDeclaration -> (ClassOrInterfaceDeclaration) typeDeclaration)
                                    .map(classOrInterfaceDeclaration -> {

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
                                                        .setName(classOrInterfaceDeclaration.getNameAsString().concat("Proxy"));


                                                CompilationUnit proxyCompilationUnit = new CompilationUnit()
                                                        .addImport(
                                                                getNameByType(moduleCompilationUnit, classOrInterfaceDeclaration),
                                                                isStaticImport(moduleCompilationUnit, classOrInterfaceDeclaration),
                                                                false
                                                        )
                                                        .addImport(Inject.class)
                                                        .addType(proxyDeclaration);

                                                interceptorBeanMethodDeclarations
                                                        .forEach(interceptorBeanMethodDeclaration -> {
                                                                    proxyCompilationUnit.addImport(
                                                                            getNameByType(moduleCompilationUnit, interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()),
                                                                            isStaticImport(moduleCompilationUnit, interceptorBeanMethodDeclaration.getType().asClassOrInterfaceType()),
                                                                            false
                                                                    );
                                                                    proxyDeclaration.addField(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE);
                                                                }
                                                        );

                                                classOrInterfaceDeclaration.getConstructors().forEach(
                                                        constructorDeclaration -> {

                                                            constructorDeclaration.getParameters()
                                                                    .forEach(parameter -> proxyCompilationUnit.addImport(
                                                                                    getNameByType(moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()),
                                                                                    isStaticImport(moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()),
                                                                                    false
                                                                            )
                                                                    );

                                                            ConstructorDeclaration proxyConstructor = proxyDeclaration
                                                                    .addConstructor(Modifier.Keyword.PUBLIC)
                                                                    .addAnnotation(Inject.class)
                                                                    .setParameters(constructorDeclaration.getParameters());

                                                            BlockStmt blockStmt = proxyConstructor.createBody()
                                                                    .addStatement("super("
                                                                            .concat(constructorDeclaration.getParameters().stream()
                                                                                    .map(NodeWithSimpleName::getNameAsString)
                                                                                    .collect(Collectors.joining(",")))
                                                                            .concat(");"));

                                                            interceptorBeanMethodDeclarations
                                                                    .forEach(interceptorBeanMethodDeclaration -> {
                                                                                proxyConstructor.addParameter(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString());
                                                                                blockStmt.addStatement("this.".concat(interceptorBeanMethodDeclaration.getNameAsString()).concat("=").concat(interceptorBeanMethodDeclaration.getNameAsString()).concat(";"));
                                                                            }
                                                                    );
                                                        }
                                                );

                                                classOrInterfaceDeclaration.getMembers().stream()
                                                        .filter(BodyDeclaration::isMethodDeclaration)
                                                        .map(BodyDeclaration::asMethodDeclaration)
                                                        .forEach(superMethodDeclaration -> {

                                                            Map<AnnotationExpr, MethodDeclaration> interceptorBeanMethodMap = superMethodDeclaration.getAnnotations().stream()
                                                                    .collect(Collectors.toMap(annotationExpr -> annotationExpr, annotationExpr -> getInterceptorBeanMethodDeclaration(moduleDeclaration, annotationExpr)))
                                                                    .entrySet().stream()
                                                                    .filter(entry -> entry.getValue().isPresent())
                                                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

                                                            if (interceptorBeanMethodMap.keySet().size() > 0) {
                                                                BlockStmt blockStmt = proxyDeclaration.addMethod(superMethodDeclaration.getNameAsString(), superMethodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                                                        .setParameters(superMethodDeclaration.getParameters())
                                                                        .setType(superMethodDeclaration.getType())
                                                                        .addAnnotation(Override.class)
                                                                        .createBody();

                                                                proxyCompilationUnit.addImport(InvocationContext.class);

                                                                interceptorBeanMethodMap.forEach((key, value) -> {

                                                                    proxyCompilationUnit.addImport(
                                                                            getNameByType(moduleCompilationUnit, key.getName()),
                                                                            isStaticImport(moduleCompilationUnit, key.getName()),
                                                                            false
                                                                    );

                                                                    StringBuilder createInvocationContext = new StringBuilder(
                                                                            "InvocationContext " + value.getName().getIdentifier() + "Context = new InvocationContext()" +
                                                                                    ".setName(\"" + superMethodDeclaration.getNameAsString() + "\")" +
                                                                                    ".setTarget(" + "this" + ")" +
                                                                                    ".setOwner(" + key.getName().getIdentifier() + ".class)"
                                                                    );

                                                                    for (Parameter parameter : superMethodDeclaration.getParameters()) {
                                                                        proxyCompilationUnit.addImport(
                                                                                getNameByType(compilationUnit, parameter.getType().asClassOrInterfaceType()),
                                                                                isStaticImport(compilationUnit, parameter.getType().asClassOrInterfaceType()),
                                                                                false
                                                                        );
                                                                        createInvocationContext.append(".addParameterValue(\"").append(parameter.getNameAsString()).append("\",").append(parameter.getNameAsString()).append(")");
                                                                    }

                                                                    for (MemberValuePair memberValuePair : key.asNormalAnnotationExpr().getPairs()) {
                                                                        annotationValueImport(proxyCompilationUnit, compilationUnit, memberValuePair.getValue());
                                                                        createInvocationContext.append(".addOwnerValue(\"").append(memberValuePair.getNameAsString()).append("\",").append(memberValuePair.getValue()).append(")");
                                                                    }

                                                                    blockStmt.addStatement(createInvocationContext.append(";").toString());
                                                                    blockStmt.addStatement("this.".concat(value.getName().getIdentifier()).concat(".before(").concat(value.getName().getIdentifier()).concat("Context);"));
                                                                });

                                                                StringBuilder processStatement = new StringBuilder();
                                                                if (!superMethodDeclaration.getType().isVoidType()) {
                                                                    processStatement.append(superMethodDeclaration.getType().asString()).append(" result =");
                                                                }
                                                                processStatement.append(" super.")
                                                                        .append(superMethodDeclaration.getNameAsString())
                                                                        .append("(")
                                                                        .append(superMethodDeclaration.getParameters().stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.joining(",")))
                                                                        .append(");");

                                                                blockStmt.addStatement(processStatement.toString());

                                                                interceptorBeanMethodMap.forEach((key, value) -> {

                                                                    if (!superMethodDeclaration.getType().isVoidType()) {
                                                                        blockStmt.addStatement(value.getName().getIdentifier().concat("Context.setReturnValue(result);"));
                                                                    }

                                                                    blockStmt.addStatement("this.".concat(value.getName().getIdentifier()).concat(".after(").concat(value.getName().getIdentifier()).concat("Context);"));

                                                                    if (!superMethodDeclaration.getType().isVoidType()) {
                                                                        blockStmt.addStatement("return result;");
                                                                    }
                                                                });
                                                            }
                                                        });
                                                return proxyCompilationUnit;
                                            }
                                    ).collect(Collectors.toList());

                            proxyCompilationUnits.forEach(
                                    this::writeToFiler
                            );

                            writeToFiler(buildProxyModule(proxyCompilationUnits, moduleDeclaration));

//                            proxyCompilationUnits.forEach(
//                                    proxyCompilationUnit -> {
//                                        writeToFiler(buildComponent(proxyCompilationUnit, moduleDeclaration));
//                                    }
//                            );
                        }
                );
    }

    protected void annotationValueImport(CompilationUnit compilationUnit, CompilationUnit sourceCompilationUnit, Expression expression) {
        if (expression.isAnnotationExpr()) {
            compilationUnit.addImport(
                    getNameByType(sourceCompilationUnit, expression.asAnnotationExpr().getName()),
                    isStaticImport(sourceCompilationUnit, expression.asAnnotationExpr().getName()),
                    false
            );
        } else if (expression.isClassExpr()) {
            compilationUnit.addImport(
                    getNameByType(sourceCompilationUnit, expression.asClassExpr().getType().asClassOrInterfaceType()),
                    isStaticImport(sourceCompilationUnit, expression.asClassExpr().getType().asClassOrInterfaceType()),
                    false
            );
        } else if (expression.isNameExpr()) {
            compilationUnit.addImport(
                    getNameByType(sourceCompilationUnit, expression.asNameExpr().getName()),
                    isStaticImport(sourceCompilationUnit, expression.asNameExpr().getName()),
                    false
            );
        }
    }

    protected CompilationUnit buildComponent(CompilationUnit proxyCompilationUnit, ClassOrInterfaceDeclaration moduleDeclaration) {

        TypeDeclaration<?> proxyTypeDeclaration = proxyCompilationUnit.getType(0);

        CompilationUnit compilationUnit = new CompilationUnit()
                .addImport(Singleton.class)
                .addImport(Component.class);

        proxyCompilationUnit.getPackageDeclaration().ifPresent(compilationUnit::setPackageDeclaration);
        moduleDeclaration.getFullyQualifiedName().ifPresent(compilationUnit::addImport);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(proxyTypeDeclaration.getNameAsString() + "Component")
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", new ClassExpr().setType(moduleDeclaration.getNameAsString())).setName(Component.class.getSimpleName()));

        classOrInterfaceDeclaration.addMethod("get").setType(proxyTypeDeclaration.getNameAsString()).removeBody();

        compilationUnit.addType(classOrInterfaceDeclaration);
        return compilationUnit;
    }


    protected CompilationUnit buildProxyModule(List<CompilationUnit> proxyCompilationUnits, ClassOrInterfaceDeclaration moduleDeclaration) {

        CompilationUnit compilationUnit = new CompilationUnit()
                .addImport(Module.class);

        proxyCompilationUnits.forEach(
                proxyCompilationUnit -> proxyCompilationUnit.getPackageDeclaration().ifPresent(compilationUnit::setPackageDeclaration)
        );
        moduleDeclaration.getFullyQualifiedName().ifPresent(compilationUnit::addImport);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleDeclaration.getNameAsString() + "Proxy")
                .addAnnotation(new NormalAnnotationExpr().addPair("includes", new ClassExpr().setType(moduleDeclaration.getNameAsString())).setName(Module.class.getSimpleName()));

        proxyCompilationUnits.stream()
                .map(proxyCompilationUnit -> proxyCompilationUnit.getType(0).asClassOrInterfaceDeclaration()).forEach(
                        proxyClassOrInterfaceDeclaration ->
                        {
                            ConstructorDeclaration injectConstructorDeclaration = proxyClassOrInterfaceDeclaration.getConstructors().stream()
                                    .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                                    .findFirst().orElseThrow();
                            classOrInterfaceDeclaration.addMethod(proxyClassOrInterfaceDeclaration.getNameAsString().toLowerCase(), Modifier.Keyword.PUBLIC)
                                    .setParameters(injectConstructorDeclaration.getParameters())
                                    .setType(proxyClassOrInterfaceDeclaration.getNameAsString())
                                    .createBody()
                                    .addStatement(
                                            "return new "
                                                    .concat(proxyClassOrInterfaceDeclaration.getNameAsString())
                                                    .concat("(")
                                                    .concat(
                                                            injectConstructorDeclaration.getParameters().stream()
                                                                    .map(NodeWithSimpleName::getNameAsString)
                                                                    .collect(Collectors.joining(","))
                                                    )
                                                    .concat(");"));
                        }
                );

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

    protected void writeToFiler(CompilationUnit compilationUnit) {
        try {
            Writer writer = filer.createSourceFile(compilationUnit.getType(0).getFullyQualifiedName().orElseGet(compilationUnit.getType(0)::getNameAsString)).openWriter();
            writer.write(compilationUnit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
