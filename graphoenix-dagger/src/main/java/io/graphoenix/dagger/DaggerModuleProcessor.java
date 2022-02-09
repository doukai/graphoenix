package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.graphoenix.spi.context.BaseModuleContext;
import io.graphoenix.spi.context.ModuleContext;
import io.vavr.Tuple2;
import jakarta.annotation.Generated;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.dagger.JavaParserUtil.JAVA_PARSER_UTIL;

@SupportedAnnotationTypes("io.graphoenix.spi.module.Module")
@AutoService(Processor.class)
public class DaggerModuleProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;
    private Set<DaggerProxyProcessor> daggerProxyProcessors;
    private Set<Class<? extends Annotation>> supports;
    private ProcessorManager processorManager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
        this.daggerProxyProcessors = ServiceLoader.load(DaggerProxyProcessor.class, DaggerModuleProcessor.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
        this.supports = new HashSet<>();
        this.processorManager = new ProcessorManager(processingEnv, this.javaParser);
        for (DaggerProxyProcessor daggerProxyProcessor : this.daggerProxyProcessors) {
            this.supports.add(daggerProxyProcessor.support());
            daggerProxyProcessor.init(processorManager);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getAnnotation(Generated.class) == null) {
                    buildComponents(trees.getPath(bundleClassElement).getCompilationUnit().toString());
                }
            }
        }
        return false;
    }

    protected void buildComponents(String sourceCode) {
        javaParser.parse(sourceCode)
                .ifSuccessful(moduleCompilationUnit -> {

                            Optional<ClassOrInterfaceDeclaration> moduleClassOrInterfaceDeclaration = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit);
                            List<CompilationUnit> componentProxyCompilationUnits = moduleClassOrInterfaceDeclaration.stream()
                                    .flatMap(moduleClassDeclaration ->
                                            moduleClassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .filter(bodyDeclaration ->
                                                            bodyDeclaration.getAnnotations().stream()
                                                                    .noneMatch(annotationExpr ->
                                                                            supports.stream().map(Class::getSimpleName).anyMatch(name -> name.equals(annotationExpr.getNameAsString()))
                                                                    )
                                                    )
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                                    .map(JAVA_PARSER_UTIL::getMethodReturnType)
                                                    .flatMap(typeStream ->
                                                            typeStream.map(componentType ->
                                                                    processorManager.getCompilationUnitByClassOrInterfaceType(moduleCompilationUnit, componentType)
                                                                            .filter(compilationUnit -> !JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit).orElseThrow().isInterface())
                                                                            .map(compilationUnit ->
                                                                                    buildComponentProxy(
                                                                                            moduleCompilationUnit,
                                                                                            compilationUnit
                                                                                    )
                                                                            )
                                                            )
                                                    )
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                    )
                                    .collect(Collectors.toList());

                            componentProxyCompilationUnits.forEach(processorManager::writeToFiler);

                            CompilationUnit moduleProxyCompilationUnit = buildModuleProxy(componentProxyCompilationUnits, moduleCompilationUnit);
                            processorManager.writeToFiler(moduleProxyCompilationUnit);

                            List<CompilationUnit> componentProxyComponentCompilationUnits = componentProxyCompilationUnits.stream()
                                    .map(componentProxyCompilationUnit ->
                                            buildComponentProxyComponent(
                                                    componentProxyCompilationUnit,
                                                    moduleProxyCompilationUnit
                                            )
                                    )
                                    .collect(Collectors.toList());

                            componentProxyComponentCompilationUnits.forEach(processorManager::writeToFiler);

                            JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleProxyCompilationUnit)
                                    .ifPresent(moduleProxyClassDeclaration -> {
                                                CompilationUnit moduleContextCompilationUnit = buildModuleContext(componentProxyComponentCompilationUnits, moduleProxyCompilationUnit, moduleProxyClassDeclaration);
                                                processorManager.writeToFiler(moduleContextCompilationUnit);
                                            }
                                    );
                        }
                );
    }

    protected CompilationUnit buildComponentProxy(CompilationUnit moduleCompilationUnit, CompilationUnit componentCompilationUnit) {
        return buildComponentProxy(
                moduleCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit).orElseThrow(),
                componentCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).orElseThrow()
        );
    }

    protected CompilationUnit buildComponentProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration) {

        ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .addModifier(Modifier.Keyword.PUBLIC)
                .addExtendedType(componentClassDeclaration.getNameAsString())
                .setName(componentClassDeclaration.getNameAsString().concat("Proxy"));

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
                                                    new NodeList<>(
                                                            constructorDeclaration.getParameters().stream()
                                                                    .map(NodeWithSimpleName::getNameAsExpression)
                                                                    .collect(Collectors.toList())
                                                    )
                                            )
                            );
                }
        );

        CompilationUnit componentProxyCompilationUnit = new CompilationUnit().addType(componentProxyClassDeclaration);
        componentCompilationUnit.getPackageDeclaration().ifPresent(componentProxyCompilationUnit::setPackageDeclaration);
        daggerProxyProcessors.forEach(daggerProxyProcessor -> daggerProxyProcessor.buildComponentProxy(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, componentClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration));
        processorManager.importAllTypesFromSource(componentProxyClassDeclaration, componentProxyCompilationUnit, moduleCompilationUnit);
        processorManager.importAllTypesFromSource(componentProxyClassDeclaration, componentProxyCompilationUnit, componentCompilationUnit);
        return componentProxyCompilationUnit;
    }

    protected CompilationUnit buildModuleProxy(List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleCompilationUnit) {
        return buildModuleProxy(
                componentProxyCompilationUnits,
                moduleCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit).orElseThrow()
        );
    }

    protected CompilationUnit buildModuleProxy(List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ClassOrInterfaceDeclaration moduleProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleClassDeclaration.getNameAsString() + "Proxy")
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr("io.graphoenix.dagger.DaggerModuleProcessor")).setName(Generated.class.getSimpleName()));

        if (moduleClassDeclaration.isInterface()) {
            moduleProxyClassDeclaration.setInterface(true);
        }

        if (moduleClassDeclaration.isAbstract()) {
            moduleProxyClassDeclaration.setAbstract(true);
        }

        CompilationUnit moduleProxyCompilationUnit = new CompilationUnit().addType(moduleProxyClassDeclaration)
                .addImport(Module.class)
                .addImport(Provides.class)
                .addImport(Generated.class)
                .addImport("io.graphoenix.core.context.BeanContext");

        moduleClassDeclaration.getMembers().stream()
                .filter(bodyDeclaration -> !bodyDeclaration.isConstructorDeclaration())
                .filter(bodyDeclaration ->
                        bodyDeclaration.getAnnotations().stream()
                                .noneMatch(annotationExpr ->
                                        supports.stream().map(Class::getSimpleName).anyMatch(name -> name.equals(annotationExpr.getNameAsString()))
                                )
                )
                .forEach(bodyDeclaration -> moduleProxyClassDeclaration.addMember(bodyDeclaration.clone()));

        componentProxyCompilationUnits
                .forEach(componentProxyCompilationUnit ->
                        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit)
                                .ifPresent(componentProxyClassDeclaration -> {

                                            MethodDeclaration componentMethodDeclaration = moduleProxyClassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .filter(declaration -> declaration.getType().isClassOrInterfaceType())
                                                    .filter(declaration -> JAVA_PARSER_UTIL.getMethodReturnType(declaration).anyMatch(type -> type.getNameAsString().equals(componentProxyClassDeclaration.getExtendedTypes(0).getNameAsString())))
                                                    .findFirst()
                                                    .orElseThrow();

                                            componentMethodDeclaration.getBody()
                                                    .ifPresent(blockStmt -> {
                                                                blockStmt.getStatements()
                                                                        .forEach(statement -> {
                                                                                    if (statement.isReturnStmt()) {
                                                                                        Expression expression = statement.asReturnStmt().getExpression().orElseThrow();
                                                                                        if (expression.isObjectCreationExpr()) {
                                                                                            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                                                            if (componentProxyClassDeclaration.getExtendedTypes().stream()
                                                                                                    .anyMatch(classOrInterfaceType -> classOrInterfaceType.getNameAsString().equals(objectCreationExpr.getType().getNameAsString()))) {
                                                                                                objectCreationExpr.setType(componentProxyClassDeclaration.getNameAsString());
                                                                                            }

                                                                                            expression.asObjectCreationExpr().getArguments().stream()
                                                                                                    .filter(Expression::isMethodCallExpr)
                                                                                                    .map(Expression::asMethodCallExpr)
                                                                                                    .forEach(methodCallExpr -> replaceComponentMethod(expression.asObjectCreationExpr().getArguments(), methodCallExpr, moduleProxyClassDeclaration));

                                                                                            expression.asObjectCreationExpr().getArguments().stream()
                                                                                                    .filter(Expression::isMethodReferenceExpr)
                                                                                                    .map(Expression::asMethodReferenceExpr)
                                                                                                    .forEach(methodReferenceExpr -> replaceComponentMethod(expression.asObjectCreationExpr().getArguments(), methodReferenceExpr, moduleProxyClassDeclaration));
                                                                                        }
                                                                                    }
                                                                                }
                                                                        );
                                                            }
                                                    );
                                        }
                                )
                );

        NormalAnnotationExpr moduleAnnotationExpr = new NormalAnnotationExpr();
        moduleAnnotationExpr.setName(Module.class.getSimpleName());
        moduleClassDeclaration.getAnnotationByClass(io.graphoenix.spi.module.Module.class).orElseThrow().clone()
                .asNormalAnnotationExpr().getPairs()
                .forEach(memberValuePair -> {
                    if (memberValuePair.getValue().isClassExpr()) {
                        memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType();
                        moduleProxyCompilationUnit.addImport(processorManager.getNameByType(moduleCompilationUnit, memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType().getName()) + "Proxy");
                        memberValuePair.getValue().asClassExpr().setType(memberValuePair.getValue().asClassExpr().getTypeAsString() + "Proxy");

                    } else if (memberValuePair.getValue().isArrayInitializerExpr()) {
                        memberValuePair.getValue().asArrayInitializerExpr().getValues().stream()
                                .filter(Expression::isClassExpr)
                                .map(Expression::asClassExpr)
                                .forEach(classExpr -> {
                                    classExpr.getType().asClassOrInterfaceType();
                                    moduleProxyCompilationUnit.addImport(processorManager.getNameByType(moduleCompilationUnit, classExpr.getType().asClassOrInterfaceType().getName()) + "Proxy");
                                    classExpr.setType(classExpr.getTypeAsString() + "Proxy");
                                });
                    }
                    moduleAnnotationExpr.addPair(memberValuePair.getNameAsString(), memberValuePair.getValue());
                });

        moduleCompilationUnit.getImports().stream().filter(importDeclaration -> importDeclaration.getNameAsString().equals(io.graphoenix.spi.module.Module.class.getName())).findFirst().ifPresent(Node::remove);
        moduleCompilationUnit.getImports().stream().filter(importDeclaration -> importDeclaration.getNameAsString().equals(io.graphoenix.spi.module.Provides.class.getName())).findFirst().ifPresent(Node::remove);

        moduleProxyClassDeclaration.addAnnotation(moduleAnnotationExpr);
        moduleCompilationUnit.getPackageDeclaration().ifPresent(moduleProxyCompilationUnit::setPackageDeclaration);
        daggerProxyProcessors.forEach(daggerProxyProcessor -> daggerProxyProcessor.buildModuleProxy(moduleCompilationUnit, moduleClassDeclaration, componentProxyCompilationUnits, moduleProxyCompilationUnit, moduleProxyClassDeclaration));

        componentProxyCompilationUnits.forEach(
                componentProxyCompilationUnit -> JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit)
                        .ifPresent(componentProxyClassDeclaration ->
                                moduleProxyCompilationUnit.addImport(
                                        componentProxyCompilationUnit.getPackageDeclaration()
                                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(componentProxyClassDeclaration.getNameAsString()))
                                                .orElseGet(componentProxyClassDeclaration::getNameAsString)
                                )
                        )
        );

        buildModuleProxyInjectField(
                moduleProxyClassDeclaration.getFields().stream()
                        .filter(fieldDeclaration -> fieldDeclaration.isAnnotationPresent(Inject.class))
                        .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                        .map(variableDeclarator ->
                                new Tuple2<>(
                                        variableDeclarator.getNameAsString(),
                                        variableDeclarator.getType()
                                )
                        ),
                moduleProxyClassDeclaration
        );

        moduleClassDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isConstructorDeclaration)
                .map(BodyDeclaration::asConstructorDeclaration)
                .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                .forEach(constructorDeclaration ->
                        buildModuleProxyInjectField(
                                constructorDeclaration.getParameters().stream()
                                        .map(parameter ->
                                                new Tuple2<>(
                                                        constructorDeclaration.getBody().getStatements().stream()
                                                                .filter(Statement::isExpressionStmt)
                                                                .map(statement -> statement.asExpressionStmt().getExpression())
                                                                .filter(Expression::isAssignExpr)
                                                                .map(Expression::asAssignExpr)
                                                                .filter(assignExpr -> assignExpr.getValue().isNameExpr())
                                                                .filter(assignExpr -> assignExpr.getValue().asNameExpr().getNameAsString().equals(parameter.getNameAsString()))
                                                                .map(AssignExpr::getTarget)
                                                                .filter(Expression::isFieldAccessExpr)
                                                                .map(expression -> expression.asFieldAccessExpr().getNameAsString())
                                                                .findFirst()
                                                                .orElseThrow(),
                                                        parameter.getType()
                                                )
                                        ),
                                moduleProxyClassDeclaration
                        )
                );

        moduleClassDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Inject.class))
                .forEach(methodDeclaration ->
                        buildModuleProxyInjectField(
                                methodDeclaration.getParameters().stream()
                                        .map(parameter ->
                                                new Tuple2<>(
                                                        methodDeclaration.getBody().orElseThrow().getStatements().stream()
                                                                .filter(Statement::isExpressionStmt)
                                                                .map(statement -> statement.asExpressionStmt().getExpression())
                                                                .filter(Expression::isAssignExpr)
                                                                .map(Expression::asAssignExpr)
                                                                .filter(assignExpr -> assignExpr.getValue().isNameExpr())
                                                                .filter(assignExpr -> assignExpr.getValue().asNameExpr().getNameAsString().equals(parameter.getNameAsString()))
                                                                .map(AssignExpr::getTarget)
                                                                .filter(Expression::isFieldAccessExpr)
                                                                .map(expression -> expression.asFieldAccessExpr().getNameAsString())
                                                                .findFirst()
                                                                .orElseThrow(),
                                                        parameter.getType()
                                                )
                                        ),
                                moduleProxyClassDeclaration
                        )
                );

        processorManager.importAllTypesFromSource(moduleProxyCompilationUnit, moduleCompilationUnit);
        return moduleProxyCompilationUnit;
    }

    private void replaceComponentMethod(NodeList<Expression> arguments, MethodCallExpr methodCallExpr, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        methodCallExpr.getArguments().stream()
                .filter(Expression::isMethodCallExpr)
                .map(Expression::asMethodCallExpr)
                .forEach(innerMethodCallExpr -> replaceComponentMethod(methodCallExpr.getArguments(), innerMethodCallExpr, moduleProxyClassDeclaration));

        if (methodCallExpr.getScope().isEmpty() || methodCallExpr.getScope().map(Expression::isThisExpr).isPresent()) {
            moduleProxyClassDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Singleton.class))
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

    private void replaceComponentMethod(NodeList<Expression> arguments, MethodReferenceExpr methodReferenceExpr, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        if (methodReferenceExpr.getScope().isThisExpr()) {
            moduleProxyClassDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Singleton.class))
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

    private void buildModuleProxyInjectField(Stream<Tuple2<String, Type>> injectFieldList, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        injectFieldList.forEach(field -> {
                    moduleProxyClassDeclaration.getMethods().stream()
                            .filter(BodyDeclaration::isMethodDeclaration)
                            .map(BodyDeclaration::asMethodDeclaration)
                            .forEach(methodDeclaration ->
                                    methodDeclaration.getBody()
                                            .ifPresent(blockStmt -> {
                                                        List<String> parameterNameList = methodDeclaration.getParameters().stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.toList());
                                                        blockStmt.getStatements().stream()
                                                                .filter(Statement::isReturnStmt)
                                                                .map(Statement::asReturnStmt)
                                                                .forEach(returnStmt -> returnStmt.getExpression().ifPresent(expression -> replaceInjectArguments(expression, parameterNameList, field)));
                                                        blockStmt.getStatements().stream()
                                                                .filter(Statement::isExpressionStmt)
                                                                .map(statement -> statement.asExpressionStmt().getExpression())
                                                                .forEach(expression -> replaceInjectArguments(expression, parameterNameList, field));
                                                    }
                                            )
                            );

                    moduleProxyClassDeclaration.getFields().stream()
                            .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                            .filter(variableDeclarator -> variableDeclarator.getNameAsString().equals(field._1()))
                            .findAny()
                            .ifPresent(Node::remove);

                    moduleProxyClassDeclaration.getFields().stream()
                            .filter(fieldDeclaration -> fieldDeclaration.getVariables().isEmpty())
                            .forEach(moduleProxyClassDeclaration::remove);
                }
        );
    }

    private void replaceInjectArguments(Expression expression, List<String> parameterNameList, Tuple2<String, Type> field) {
        List<Expression> arguments = Collections.emptyList();

        if (expression.isObjectCreationExpr()) {
            arguments = new ArrayList<>(expression.asObjectCreationExpr().getArguments());
        } else if (expression.isMethodCallExpr()) {
            arguments = new ArrayList<>(expression.asMethodCallExpr().getArguments());
        }

        arguments.stream().filter(Expression::isNameExpr)
                .map(Expression::asNameExpr)
                .filter(argument -> !parameterNameList.contains(argument.getNameAsString()))
                .filter(argument -> field._1().equals(argument.getNameAsString()))
                .forEach(argument -> {
                            if (field._2().isClassOrInterfaceType()) {
                                if (field._2().asClassOrInterfaceType().getNameAsString().equals(Provider.class.getSimpleName())) {
                                    expression.replace(
                                            argument,
                                            new MethodCallExpr()
                                                    .setName("getProvider")
                                                    .setScope(new NameExpr().setName("BeanContext"))
                                                    .addArgument(new ClassExpr().setType(field._2().asClassOrInterfaceType().getTypeArguments().orElseThrow().get(0)))
                                    );
                                } else {
                                    expression.replace(
                                            argument,
                                            new MethodCallExpr()
                                                    .setName("get")
                                                    .setScope(new NameExpr().setName("BeanContext"))
                                                    .addArgument(new ClassExpr().setType(field._2().asClassOrInterfaceType()))
                                    );
                                }
                            }
                        }
                );

        arguments.stream().filter(Expression::isMethodCallExpr)
                .map(Expression::asMethodCallExpr)
                .filter(methodCallExpr -> methodCallExpr.getScope().isPresent())
                .filter(methodCallExpr -> methodCallExpr.getScope().get().isNameExpr())
                .map(methodCallExpr -> methodCallExpr.getScope().get().asNameExpr())
                .filter(argument -> !parameterNameList.contains(argument.getNameAsString()))
                .filter(argument -> field._1().equals(argument.getNameAsString()))
                .forEach(argument -> {
                            if (field._2().isClassOrInterfaceType()) {
                                if (field._2().asClassOrInterfaceType().getNameAsString().equals(Provider.class.getSimpleName())) {
                                    expression.replace(
                                            argument.getParentNode().orElseThrow(),
                                            new MethodCallExpr()
                                                    .setName("get")
                                                    .setScope(
                                                            new MethodCallExpr()
                                                                    .setName("getProvider")
                                                                    .setScope(new NameExpr().setName("BeanContext"))
                                                                    .addArgument(new ClassExpr().setType(field._2().asClassOrInterfaceType().getTypeArguments().orElseThrow().get(0)))
                                                    )
                                    );
                                }
                            }
                        }
                );
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, CompilationUnit moduleProxyCompilationUnit) {
        return buildComponentProxyComponent(
                componentProxyCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).orElseThrow(),
                moduleProxyCompilationUnit,
                JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(moduleProxyCompilationUnit).orElseThrow()
        );
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleProxyClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentProxyClassDeclaration.getNameAsString() + "Component")
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        componentProxyComponentInterfaceDeclaration
                .addMethod("get")
                .setType(
                        moduleProxyClassDeclaration.getMembers().stream()
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
                .addType(componentProxyComponentInterfaceDeclaration)
                .addImport(Singleton.class)
                .addImport(Component.class);

        componentProxyCompilationUnit.getPackageDeclaration().ifPresent(componentProxyComponentCompilationUnit::setPackageDeclaration);
        processorManager.importAllTypesFromSource(componentProxyComponentInterfaceDeclaration, componentProxyComponentCompilationUnit, componentProxyCompilationUnit);
        componentProxyComponentCompilationUnit.addImport(
                moduleProxyCompilationUnit.getPackageDeclaration()
                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(moduleProxyClassDeclaration.getNameAsString()))
                        .orElseGet(moduleProxyClassDeclaration::getNameAsString)
        );

        daggerProxyProcessors.forEach(daggerProxyProcessor -> daggerProxyProcessor.buildComponentProxyComponent(moduleProxyCompilationUnit, moduleProxyClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration, componentProxyComponentCompilationUnit, componentProxyComponentInterfaceDeclaration));
        processorManager.importAllTypesFromSource(componentProxyComponentInterfaceDeclaration, componentProxyComponentCompilationUnit, moduleProxyCompilationUnit);
        return componentProxyComponentCompilationUnit;
    }


    private CompilationUnit buildModuleContext(List<CompilationUnit> componentProxyComponentCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

        ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleProxyClassDeclaration.getNameAsString().concat("Context"))
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

        moduleProxyCompilationUnit.getPackageDeclaration().ifPresent(moduleContextComponentCompilationUnit::setPackageDeclaration);

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
