package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import com.sun.source.util.Trees;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.graphoenix.spi.context.BaseModuleContext;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.annotation.Generated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.dagger.JavaParserUtil.JAVA_PARSER_UTIL;

@SupportedAnnotationTypes({
        "jakarta.inject.Singleton",
        "jakarta.enterprise.context.Dependent",
        "jakarta.enterprise.context.ApplicationScoped"
})
@AutoService(Processor.class)
public class InjectProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;
    private Set<DaggerProxyProcessor> daggerProxyProcessors;
    private Set<Class<? extends Annotation>> supports;
    private ProcessorManager processorManager;
    private RoundEnvironment roundEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
        this.daggerProxyProcessors = ServiceLoader.load(DaggerProxyProcessor.class, DaggerModuleProcessor.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
        this.supports = new HashSet<>();
        this.processorManager = new ProcessorManager(processingEnv);
        for (DaggerProxyProcessor daggerProxyProcessor : this.daggerProxyProcessors) {
            this.supports.add(daggerProxyProcessor.support());
            daggerProxyProcessor.init(processorManager);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> singletonSet = roundEnv.getElementsAnnotatedWith(Singleton.class);
        Set<? extends Element> dependentSet = roundEnv.getElementsAnnotatedWith(Dependent.class);
        Set<? extends Element> applicationScopedSet = roundEnv.getElementsAnnotatedWith(ApplicationScoped.class);

        boolean anyMatch = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .anyMatch(element -> element.getKind().isClass());

        if (!anyMatch) {
            return false;
        }

        this.roundEnv = roundEnv;

        List<CompilationUnit> componentProxyCompilationUnits = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> buildComponentProxy(trees.getPath(typeElement).getCompilationUnit().toString()))
                .collect(Collectors.toList());
        componentProxyCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));

        CompilationUnit moduleCompilationUnit = buildModule(singletonSet, dependentSet, applicationScopedSet, componentProxyCompilationUnits);
        processorManager.writeToFiler(moduleCompilationUnit);

        List<CompilationUnit> componentProxyComponentCompilationUnits = componentProxyCompilationUnits.stream()
                .map(componentProxyCompilationUnit -> buildComponentProxyComponent(componentProxyCompilationUnit, moduleCompilationUnit))
                .collect(Collectors.toList());
        componentProxyComponentCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));

        CompilationUnit moduleContextCompilationUnit = buildModuleContext(componentProxyComponentCompilationUnits, moduleCompilationUnit);
        processorManager.writeToFiler(moduleContextCompilationUnit);


        List<CompilationUnit> producesComponentProxyCompilationUnits = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .flatMap(typeElement -> buildProducesComponentProxyStream(trees.getPath(typeElement).getCompilationUnit().toString()))
                .collect(Collectors.toList());

        producesComponentProxyCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));

        buildProducesModuleStream(singletonSet, dependentSet, applicationScopedSet, producesComponentProxyCompilationUnits)
                .forEach(producesModuleCompilationUnit -> {
                            processorManager.writeToFiler(producesModuleCompilationUnit);

                            List<CompilationUnit> producesComponentProxyComponentCompilationUnits = producesComponentProxyCompilationUnits.stream()
                                    .filter(producesComponentProxyCompilationUnit -> {
                                                ClassOrInterfaceDeclaration producesComponentProxyClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(producesComponentProxyCompilationUnit).orElseThrow();
                                                ClassOrInterfaceDeclaration producesModuleClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(producesModuleCompilationUnit).orElseThrow();
                                                return producesModuleClassDeclaration.getMethods().stream()
                                                        .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Provides.class))
                                                        .flatMap(JAVA_PARSER_UTIL::getMethodReturnType)
                                                        .anyMatch(classOrInterfaceType ->
                                                                processorManager.getNameByType(producesModuleCompilationUnit, classOrInterfaceType)
                                                                        .equals(producesComponentProxyClassDeclaration.getFullyQualifiedName().orElse(producesComponentProxyClassDeclaration.getNameAsString()))
                                                        );
                                            }
                                    )
                                    .map(componentProxyCompilationUnit -> buildComponentProxyComponent(componentProxyCompilationUnit, producesModuleCompilationUnit))
                                    .collect(Collectors.toList());
                            producesComponentProxyComponentCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));

                            CompilationUnit producesModuleContextCompilationUnit = buildModuleContext(producesComponentProxyComponentCompilationUnits, producesModuleCompilationUnit);
                            processorManager.writeToFiler(producesModuleContextCompilationUnit);
                        }
                );

        return false;
    }

    protected CompilationUnit getCompilationUnit(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().orElseThrow();
    }

    protected CompilationUnit buildComponentProxy(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().map(this::buildComponentProxy).orElseThrow();
    }

    protected CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit) {
        return buildComponentProxy(
                componentCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow()
        );
    }

    protected CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration) {

        ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .addModifier(Modifier.Keyword.PUBLIC)
                .addExtendedType(componentClassDeclaration.getNameAsString())
                .setName(componentClassDeclaration.getNameAsString().concat("Proxy"))
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

        componentClassDeclaration.getConstructors().forEach(
                constructorDeclaration -> {
                    ConstructorDeclaration componentProxyClassConstructor = componentProxyClassDeclaration
                            .addConstructor(Modifier.Keyword.PUBLIC)
                            .setAnnotations(constructorDeclaration.getAnnotations())
                            .setParameters(constructorDeclaration.getParameters());

                    componentProxyClassConstructor
                            .createBody()
                            .addStatement(
                                    new MethodCallExpr()
                                            .setName(new SuperExpr().toString())
                                            .setArguments(
                                                    constructorDeclaration.getParameters().stream()
                                                            .map(NodeWithSimpleName::getNameAsExpression)
                                                            .collect(Collectors.toCollection(NodeList::new))
                                            )
                            );
                }
        );

        CompilationUnit componentProxyCompilationUnit = new CompilationUnit().addType(componentProxyClassDeclaration).addImport(Generated.class);
        componentCompilationUnit.getPackageDeclaration().ifPresent(componentProxyCompilationUnit::setPackageDeclaration);
        processorManager.importAllTypesFromSource(componentProxyCompilationUnit, componentCompilationUnit);
        return componentProxyCompilationUnit;
    }

    protected Stream<CompilationUnit> buildProducesComponentProxyStream(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().map(this::buildProducesComponentProxyStream).orElseThrow();
    }

    protected Stream<CompilationUnit> buildProducesComponentProxyStream(CompilationUnit componentCompilationUnit) {
        return buildProducesComponentProxyStream(
                componentCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow()
        );
    }

    protected Stream<CompilationUnit> buildProducesComponentProxyStream(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration) {
        return componentClassDeclaration.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                .flatMap(JAVA_PARSER_UTIL::getMethodReturnType)
                .map(classOrInterfaceType -> processorManager.getCompilationUnitByClassOrInterfaceType(componentCompilationUnit, classOrInterfaceType).orElseThrow())
                .map(this::buildComponentProxy);
    }

    protected Stream<CompilationUnit> buildProducesModuleStream(Set<? extends Element> singletonSet,
                                                                Set<? extends Element> dependentSet,
                                                                Set<? extends Element> applicationScopedSet,
                                                                List<CompilationUnit> producesComponentProxyCompilationUnits) {

        return Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> getCompilationUnit(trees.getPath(typeElement).getCompilationUnit().toString()))
                .filter(compilationUnit ->
                        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow().getMethods().stream()
                                .anyMatch(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                )
                .map(compilationUnit -> {
                            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow();
                            ClassOrInterfaceDeclaration moduleClassDeclaration = new ClassOrInterfaceDeclaration()
                                    .setPublic(true)
                                    .setName(classOrInterfaceDeclaration.getNameAsString().concat("Module"))
                                    .addAnnotation(Module.class)
                                    .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

                            CompilationUnit moduleCompilationUnit = new CompilationUnit().addType(moduleClassDeclaration)
                                    .addImport(Module.class)
                                    .addImport(Provides.class)
                                    .addImport(Singleton.class)
                                    .addImport(Generated.class)
                                    .addImport("io.graphoenix.core.context.BeanContext");

                            compilationUnit.getPackageDeclaration().ifPresent(moduleCompilationUnit::setPackageDeclaration);
                            moduleClassDeclaration.setExtendedTypes(classOrInterfaceDeclaration.getExtendedTypes());
                            moduleClassDeclaration.setImplementedTypes(classOrInterfaceDeclaration.getImplementedTypes());
                            moduleClassDeclaration.setMembers(classOrInterfaceDeclaration.getMembers());
                            moduleClassDeclaration.getConstructors().forEach(constructorDeclaration -> constructorDeclaration.setName(moduleClassDeclaration.getNameAsString()));

                            moduleClassDeclaration.getFields().stream()
                                    .filter(fieldDeclaration -> fieldDeclaration.isAnnotationPresent(Inject.class))
                                    .forEach(fieldDeclaration -> {
                                                fieldDeclaration.getVariables()
                                                        .forEach(variableDeclarator -> {
                                                                    if (variableDeclarator.getType().isClassOrInterfaceType()) {
                                                                        if (processorManager.getNameByType(compilationUnit, variableDeclarator.getType().asClassOrInterfaceType()).equals(Provider.class.getName())) {
                                                                            variableDeclarator.setInitializer(
                                                                                    new MethodCallExpr()
                                                                                            .setName("getProvider")
                                                                                            .setScope(new NameExpr().setName("BeanContext"))
                                                                                            .addArgument(new ClassExpr().setType(fieldDeclaration.getElementType().asClassOrInterfaceType().getTypeArguments().orElseThrow().get(0)))
                                                                            );
                                                                            moduleCompilationUnit.addImport(Provider.class);
                                                                        } else {
                                                                            variableDeclarator.setInitializer(
                                                                                    new MethodCallExpr()
                                                                                            .setName("get")
                                                                                            .setScope(new NameExpr().setName("BeanContext"))
                                                                                            .addArgument(new ClassExpr().setType(fieldDeclaration.getElementType()))
                                                                            );
                                                                        }
                                                                    }

                                                                }
                                                        );
                                                fieldDeclaration.getAnnotationByClass(Inject.class).ifPresent(Node::remove);
                                            }
                                    );

                            moduleClassDeclaration.getConstructors().stream()
                                    .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                                    .forEach(constructorDeclaration -> {
                                                constructorDeclaration.getBody().getStatements().stream()
                                                        .filter(Statement::isExpressionStmt)
                                                        .map(statement -> statement.asExpressionStmt().getExpression())
                                                        .filter(Expression::isAssignExpr)
                                                        .map(Expression::asAssignExpr)
                                                        .filter(assignExpr -> assignExpr.getTarget().isFieldAccessExpr())
                                                        .filter(assignExpr ->
                                                                assignExpr.getTarget().asFieldAccessExpr().getScope() == null ||
                                                                        assignExpr.getTarget().asFieldAccessExpr().getScope().isThisExpr()
                                                        )
                                                        .filter(assignExpr -> assignExpr.getValue().isNameExpr())
                                                        .forEach(assignExpr -> {
                                                                    constructorDeclaration.getParameters().stream()
                                                                            .filter(parameter -> parameter.getNameAsString().equals(assignExpr.getValue().asNameExpr().getNameAsString()))
                                                                            .forEach(parameter -> {
                                                                                        if (processorManager.getNameByType(compilationUnit, parameter.getType().asClassOrInterfaceType()).equals(Provider.class.getName())) {
                                                                                            assignExpr.setValue(
                                                                                                    new MethodCallExpr()
                                                                                                            .setName("getProvider")
                                                                                                            .setScope(new NameExpr().setName("BeanContext"))
                                                                                                            .addArgument(new ClassExpr().setType(parameter.getType().asClassOrInterfaceType().getTypeArguments().orElseThrow().get(0)))
                                                                                            );
                                                                                            moduleCompilationUnit.addImport(Provider.class);
                                                                                        } else {
                                                                                            assignExpr.setValue(
                                                                                                    new MethodCallExpr()
                                                                                                            .setName("get")
                                                                                                            .setScope(new NameExpr().setName("BeanContext"))
                                                                                                            .addArgument(new ClassExpr().setType(parameter.getType()))
                                                                                            );
                                                                                        }

                                                                                    }
                                                                            );
                                                                    constructorDeclaration.getParameters().clear();
                                                                    constructorDeclaration.getAnnotationByClass(Inject.class).ifPresent(Node::remove);
                                                                }
                                                        );
                                            }
                                    );

                            moduleClassDeclaration.getMethods().stream()
                                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                                    .forEach(producesMethodDeclaration -> {
                                                producesMethodDeclaration.addAnnotation(Provides.class);
                                                if (producesMethodDeclaration.isAnnotationPresent(ApplicationScoped.class)) {
                                                    producesMethodDeclaration.addAnnotation(Singleton.class);
                                                }
                                                producesMethodDeclaration.getAnnotationByClass(Produces.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(ApplicationScoped.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(Dependent.class).ifPresent(Node::remove);

                                                producesMethodDeclaration.getBody()
                                                        .ifPresent(blockStmt ->
                                                                blockStmt.getStatements()
                                                                        .forEach(statement -> {
                                                                                    if (statement.isReturnStmt()) {
                                                                                        Expression expression = statement.asReturnStmt().getExpression().orElseThrow();
                                                                                        if (expression.isObjectCreationExpr()) {
                                                                                            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                                                            CompilationUnit componentCompilationUnit = processorManager.getCompilationUnitByClassOrInterfaceType(compilationUnit, objectCreationExpr.getType()).orElseThrow();
                                                                                            CompilationUnit componentProxyCompilationUnit = getProducesComponentProxyCompilationUnit(componentCompilationUnit, producesComponentProxyCompilationUnits);
                                                                                            ClassOrInterfaceDeclaration componentProxyClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow();
                                                                                            objectCreationExpr.setType(componentProxyClassDeclaration.getNameAsString());

                                                                                            objectCreationExpr.getArguments().stream()
                                                                                                    .filter(Expression::isMethodCallExpr)
                                                                                                    .map(Expression::asMethodCallExpr)
                                                                                                    .forEach(methodCallExpr -> replaceComponentMethod(objectCreationExpr.getArguments(), methodCallExpr, classOrInterfaceDeclaration));

                                                                                            objectCreationExpr.getArguments().stream()
                                                                                                    .filter(Expression::isMethodReferenceExpr)
                                                                                                    .map(Expression::asMethodReferenceExpr)
                                                                                                    .forEach(methodReferenceExpr -> replaceComponentMethod(objectCreationExpr.getArguments(), methodReferenceExpr, classOrInterfaceDeclaration));

                                                                                            processorManager.importAllTypesFromSource(moduleCompilationUnit, componentCompilationUnit);
                                                                                            processorManager.importAllTypesFromSource(moduleCompilationUnit, componentProxyCompilationUnit);
                                                                                        }
                                                                                    }
                                                                                }
                                                                        )
                                                        );
                                            }
                                    );
                            return moduleCompilationUnit;
                        }
                );
    }

    protected CompilationUnit buildModule(Set<? extends Element> singletonSet,
                                          Set<? extends Element> dependentSet,
                                          Set<? extends Element> applicationScopedSet,
                                          List<CompilationUnit> componentProxyCompilationUnits) {

        ClassOrInterfaceDeclaration moduleClassDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName("PackageModule")
                .addAnnotation(Module.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

        CompilationUnit moduleCompilationUnit = new CompilationUnit().addType(moduleClassDeclaration)
                .addImport(Module.class)
                .addImport(Provides.class)
                .addImport(Singleton.class)
                .addImport(Generated.class)
                .addImport("io.graphoenix.core.context.BeanContext");

        roundEnv.getRootElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.PACKAGE))
                .map(element -> (PackageElement) element)
                .findFirst()
                .ifPresent(packageElement -> moduleCompilationUnit.setPackageDeclaration(packageElement.getQualifiedName().toString()));

        Streams.concat(singletonSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> getCompilationUnit(trees.getPath(typeElement).getCompilationUnit().toString()))
                .forEach(componentCompilationUnit -> buildProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, true, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        dependentSet.stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> getCompilationUnit(trees.getPath(typeElement).getCompilationUnit().toString()))
                .forEach(componentCompilationUnit -> buildProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, false, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        return moduleCompilationUnit;
    }

    private void replaceComponentMethod(NodeList<Expression> arguments, MethodCallExpr methodCallExpr, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

        methodCallExpr.getArguments().stream()
                .filter(Expression::isMethodCallExpr)
                .map(Expression::asMethodCallExpr)
                .forEach(innerMethodCallExpr -> replaceComponentMethod(methodCallExpr.getArguments(), innerMethodCallExpr, classOrInterfaceDeclaration));

        if (methodCallExpr.getScope().isEmpty() || methodCallExpr.getScope().map(Expression::isThisExpr).isPresent()) {
            classOrInterfaceDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodCallExpr.getNameAsString()))
                    .findAny()
                    .ifPresent(methodDeclaration ->
                            arguments.replace(
                                    methodCallExpr,
                                    new MethodCallExpr()
                                            .setName("get")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(methodDeclaration.getType()))
                            )
                    );
        }
    }

    private void replaceComponentMethod(NodeList<Expression> arguments, MethodReferenceExpr methodReferenceExpr, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

        if (methodReferenceExpr.getScope().isThisExpr()) {
            classOrInterfaceDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodReferenceExpr.getIdentifier()))
                    .findAny()
                    .ifPresent(methodDeclaration ->
                            arguments.replace(
                                    methodReferenceExpr,
                                    new MethodCallExpr()
                                            .setName("getProvider")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(methodDeclaration.getType()))
                            )
                    );
        }
    }

    protected CompilationUnit getComponentProxyCompilationUnit(CompilationUnit componentCompilationUnit, List<CompilationUnit> componentProxyCompilationUnits) {
        return componentProxyCompilationUnits.stream()
                .filter(componentProxyCompilationUnit ->
                        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow().getExtendedTypes().stream()
                                .anyMatch(classOrInterfaceType -> classOrInterfaceType.getNameAsString().equals(JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow().getNameAsString()))
                )
                .findFirst()
                .orElseThrow();
    }

    protected CompilationUnit getProducesComponentProxyCompilationUnit(CompilationUnit componentCompilationUnit, List<CompilationUnit> producesComponentProxyCompilationUnits) {
        return producesComponentProxyCompilationUnits.stream()
                .filter(componentProxyCompilationUnit ->
                        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow().getExtendedTypes().stream()
                                .anyMatch(classOrInterfaceType -> classOrInterfaceType.getNameAsString().equals(JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow().getNameAsString()))
                )
                .findFirst()
                .orElseThrow();
    }

    protected void buildProvidesMethod(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, boolean isSingleton, CompilationUnit componentProxyCompilationUnit) {
        ClassOrInterfaceDeclaration componentClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow();
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow();

        moduleCompilationUnit.addImport(componentProxyClassDeclaration.getFullyQualifiedName().orElse(componentProxyClassDeclaration.getNameAsString()));

        MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, componentClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                .addAnnotation(Provides.class)
                .setType(componentClassDeclaration.getNameAsString());
        if (isSingleton) {
            methodDeclaration.addAnnotation(Singleton.class);
        }

        methodDeclaration.createBody()
                .addStatement(
                        new ReturnStmt(
                                new ObjectCreationExpr()
                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                        .setArguments(
                                                componentClassDeclaration.getConstructors().stream()
                                                        .findFirst()
                                                        .map(constructorDeclaration ->
                                                                constructorDeclaration.getParameters().stream()
                                                                        .map(parameter -> {
                                                                                    ClassOrInterfaceType classOrInterfaceType = parameter.getType().asClassOrInterfaceType();
                                                                                    if (processorManager.getNameByType(componentCompilationUnit, classOrInterfaceType).equals(Provider.class.getName())) {
                                                                                        return new MethodCallExpr()
                                                                                                .setName("getProvider")
                                                                                                .setScope(new NameExpr().setName("BeanContext"))
                                                                                                .addArgument(new ClassExpr().setType(classOrInterfaceType.getTypeArguments().orElseThrow().get(0)));
                                                                                    } else {
                                                                                        return new MethodCallExpr()
                                                                                                .setName("get")
                                                                                                .setScope(new NameExpr().setName("BeanContext"))
                                                                                                .addArgument(new ClassExpr().setType(classOrInterfaceType));
                                                                                    }
                                                                                }
                                                                        )
                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                        )
                                                        .orElseThrow()
                                        )
                        )
                );

        processorManager.importAllTypesFromSource(moduleCompilationUnit, componentCompilationUnit);
        processorManager.importAllTypesFromSource(moduleCompilationUnit, componentProxyCompilationUnit);
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, CompilationUnit moduleCompilationUnit) {
        return buildComponentProxyComponent(
                componentProxyCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow(),
                moduleCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit).orElseThrow()
        );
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration componentProxyFactoryInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentProxyClassDeclaration.getNameAsString() + "Component")
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        componentProxyFactoryInterfaceDeclaration
                .addMethod("get")
                .setType(
                        moduleClassDeclaration.getMembers().stream()
                                .filter(BodyDeclaration::isMethodDeclaration)
                                .map(BodyDeclaration::asMethodDeclaration)
                                .filter(methodDeclaration ->
                                        JAVA_PARSER_UTIL.getMethodReturnType(methodDeclaration)
                                                .anyMatch(type ->
                                                        type.getNameAsString().equals(componentProxyClassDeclaration.getNameAsString()) ||
                                                                componentProxyClassDeclaration.getExtendedTypes().stream().anyMatch(extendType -> extendType.getNameAsString().equals(type.getNameAsString())))
                                )
                                .findFirst()
                                .orElseThrow()
                                .getType()
                ).removeBody();

        CompilationUnit componentProxyComponentCompilationUnit = new CompilationUnit()
                .addType(componentProxyFactoryInterfaceDeclaration)
                .addImport(Component.class);

        componentProxyCompilationUnit.getPackageDeclaration().ifPresent(componentProxyComponentCompilationUnit::setPackageDeclaration);
        processorManager.importAllTypesFromSource(componentProxyComponentCompilationUnit, componentProxyCompilationUnit);
        componentProxyComponentCompilationUnit.addImport(
                moduleCompilationUnit.getPackageDeclaration()
                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(moduleClassDeclaration.getNameAsString()))
                        .orElseGet(moduleClassDeclaration::getNameAsString)
        );

        processorManager.importAllTypesFromSource(componentProxyComponentCompilationUnit, moduleCompilationUnit);
        return componentProxyComponentCompilationUnit;
    }

    private CompilationUnit buildModuleContext(List<CompilationUnit> componentProxyComponentCompilationUnits, CompilationUnit moduleCompilationUnit) {
        return buildModuleContext(componentProxyComponentCompilationUnits, moduleCompilationUnit, JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit).orElseThrow());
    }

    private CompilationUnit buildModuleContext(List<CompilationUnit> componentProxyComponentCompilationUnits, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleClassDeclaration.getNameAsString().concat("Context"))
                .addAnnotation(
                        new SingleMemberAnnotationExpr()
                                .setMemberValue(new ClassExpr().setType(ModuleContext.class))
                                .setName(AutoService.class.getSimpleName())
                )
                .addExtendedType(BaseModuleContext.class);

        CompilationUnit moduleContextComponentCompilationUnit = new CompilationUnit()
                .addType(moduleContextInterfaceDeclaration)
                .addImport(AutoService.class)
                .addImport(ModuleContext.class)
                .addImport(BaseModuleContext.class);

        moduleCompilationUnit.getPackageDeclaration().ifPresent(moduleContextComponentCompilationUnit::setPackageDeclaration);

        BlockStmt blockStmt = moduleContextInterfaceDeclaration.addStaticInitializer();

        componentProxyComponentCompilationUnits.forEach(
                componentProxyComponentCompilationUnit -> {

                    ClassOrInterfaceDeclaration componentProxyComponentClassDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyComponentCompilationUnit).orElseThrow();

                    String daggerClassName = "Dagger".concat(componentProxyComponentClassDeclaration.getNameAsString());
                    String daggerVariableName = "dagger".concat(componentProxyComponentClassDeclaration.getNameAsString());

                    ClassOrInterfaceType componentType = componentProxyComponentClassDeclaration.getMembers().stream()
                            .filter(BodyDeclaration::isMethodDeclaration)
                            .map(BodyDeclaration::asMethodDeclaration)
                            .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals("get"))
                            .map(MethodDeclaration::getType)
                            .filter(Type::isClassOrInterfaceType)
                            .map(Type::asClassOrInterfaceType)
                            .findFirst()
                            .orElseThrow();

                    moduleContextComponentCompilationUnit.addImport(processorManager.getNameByType(componentProxyComponentCompilationUnit, componentType));

                    blockStmt.addStatement(new VariableDeclarationExpr()
                            .addVariable(
                                    new VariableDeclarator()
                                            .setType(componentProxyComponentClassDeclaration.getNameAsString())
                                            .setName(daggerVariableName)
                                            .setInitializer(
                                                    new MethodCallExpr()
                                                            .setName("create")
                                                            .setScope(new NameExpr().setName(daggerClassName))
                                            )
                            )
                    );

                    blockStmt.addStatement(
                            new MethodCallExpr()
                                    .setName("put")
                                    .addArgument(new ClassExpr().setType(componentType))
                                    .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
                    );

                    componentProxyComponentCompilationUnit.getPackageDeclaration()
                            .ifPresent(packageDeclaration -> {
                                        moduleContextComponentCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(componentProxyComponentClassDeclaration.getNameAsString()));
                                        moduleContextComponentCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(daggerClassName));
                                    }
                            );
                }
        );

        return moduleContextComponentCompilationUnit;
    }
}
