package io.graphoenix.inject;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.graphoenix.inject.error.InjectionProcessException;
import io.graphoenix.spi.context.BaseModuleContext;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.annotation.Generated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.inject.error.InjectionProcessErrorType.CANNOT_GET_COMPILATION_UNIT;
import static io.graphoenix.inject.error.InjectionProcessErrorType.COMPONENT_GET_METHOD_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.CONSTRUCTOR_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.MODULE_PROVIDERS_METHOD_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.INSTANCE_TYPE_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.PROVIDER_TYPE_NOT_EXIST;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes({
        "jakarta.inject.Singleton",
        "jakarta.enterprise.context.Dependent",
        "jakarta.enterprise.context.ApplicationScoped",
        "jakarta.enterprise.context.RequestScoped",
        "jakarta.enterprise.context.SessionScoped"
})
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class InjectProcessor extends AbstractProcessor {

    private Set<ComponentProxyProcessor> componentProxyProcessors;
    private ProcessorManager processorManager;
    private List<String> processed;
    private AtomicInteger round;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processed = new CopyOnWriteArrayList<>();
        round = new AtomicInteger(0);
        this.componentProxyProcessors = ServiceLoader.load(ComponentProxyProcessor.class, InjectProcessor.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
        Logger.info("{} component processor loaded", componentProxyProcessors.size());
        this.processorManager = new ProcessorManager(processingEnv, InjectProcessor.class.getClassLoader());
        for (ComponentProxyProcessor componentProxyProcessor : this.componentProxyProcessors) {
            Logger.debug("init {}", componentProxyProcessor.getClass().getName());
            componentProxyProcessor.init(processorManager);
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
        Set<? extends Element> requestScopedSet = roundEnv.getElementsAnnotatedWith(RequestScoped.class);
        Set<? extends Element> sessionScopedSet = roundEnv.getElementsAnnotatedWith(SessionScoped.class);

        List<TypeElement> typeElements = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream(), requestScopedSet.stream(), sessionScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .filter(typeElement -> !processed.contains(typeElement.getQualifiedName().toString()))
                .collect(Collectors.toList());

        if (typeElements.size() == 0) {
            return false;
        } else {
            processed.addAll(typeElements.stream().map(typeElement -> typeElement.getQualifiedName().toString()).collect(Collectors.toList()));
        }

        processorManager.setRoundEnv(roundEnv);
        componentProxyProcessors
                .forEach(componentProxyProcessor -> {
                            Logger.debug("inProcess {}", componentProxyProcessor.getClass().getName());
                            componentProxyProcessor.inProcess();
                        }
                );

        List<CompilationUnit> componentProxyCompilationUnits = typeElements.stream()
                .map(this::buildComponentProxy)
                .collect(Collectors.toList());
        componentProxyCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));
        Logger.debug("all proxy class build success");

        CompilationUnit moduleCompilationUnit = buildModule(singletonSet, dependentSet, applicationScopedSet, requestScopedSet, sessionScopedSet, componentProxyCompilationUnits);
        processorManager.writeToFiler(moduleCompilationUnit);
        Logger.debug("module class build success");

        List<CompilationUnit> componentProxyComponentCompilationUnits = componentProxyCompilationUnits.stream()
                .map(componentProxyCompilationUnit -> buildComponentProxyComponent(componentProxyCompilationUnit, moduleCompilationUnit))
                .collect(Collectors.toList());
        componentProxyComponentCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));
        Logger.debug("all proxy component class build success");

        CompilationUnit moduleContextCompilationUnit = buildModuleContext(componentProxyComponentCompilationUnits, moduleCompilationUnit);
        processorManager.writeToFiler(moduleContextCompilationUnit);
        Logger.debug("module context class build success");

        List<CompilationUnit> producesComponentProxyCompilationUnits = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream(), requestScopedSet.stream(), sessionScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .flatMap(this::buildProducesComponentProxyStream)
                .collect(Collectors.toList());

        producesComponentProxyCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));
        Logger.debug("all produces method proxy class build success");

        buildProducesModuleStream(singletonSet, dependentSet, applicationScopedSet, requestScopedSet, sessionScopedSet, producesComponentProxyCompilationUnits)
                .forEach(producesModuleCompilationUnit -> {
                            processorManager.writeToFiler(producesModuleCompilationUnit);
                            List<CompilationUnit> producesComponentProxyComponentCompilationUnits = producesComponentProxyCompilationUnits.stream()
                                    .filter(producesComponentProxyCompilationUnit -> {
                                                ClassOrInterfaceDeclaration producesComponentProxyClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(producesComponentProxyCompilationUnit);
                                                ClassOrInterfaceDeclaration producesModuleClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(producesModuleCompilationUnit);
                                                return producesModuleClassDeclaration.getMethods().stream()
                                                        .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Provides.class))
                                                        .flatMap(processorManager::getMethodReturnType)
                                                        .filter(ResolvedType::isReferenceType)
                                                        .anyMatch(resolvedType -> {
                                                                    if (resolvedType.asReferenceType().getQualifiedName().equals(PublisherBuilder.class.getName())) {
                                                                        ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType().getTypeParametersMap().get(0).b.asReferenceType();
                                                                        return resolvedReferenceType.getQualifiedName().equals(producesComponentProxyClassDeclaration.getFullyQualifiedName().orElse(producesComponentProxyClassDeclaration.getNameAsString())) ||
                                                                                producesComponentProxyClassDeclaration.getExtendedTypes().stream()
                                                                                        .anyMatch(classOrInterfaceType -> processorManager.getQualifiedNameByType(classOrInterfaceType).equals(resolvedReferenceType.getQualifiedName()));
                                                                    } else {
                                                                        return resolvedType.asReferenceType().getQualifiedName().equals(producesComponentProxyClassDeclaration.getFullyQualifiedName().orElse(producesComponentProxyClassDeclaration.getNameAsString())) ||
                                                                                producesComponentProxyClassDeclaration.getExtendedTypes().stream()
                                                                                        .anyMatch(classOrInterfaceType -> processorManager.getQualifiedNameByType(classOrInterfaceType).equals(resolvedType.asReferenceType().getQualifiedName()));
                                                                    }
                                                                }
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
        Logger.debug("all produces module class build success");
        round.incrementAndGet();
        return false;
    }

    private CompilationUnit buildComponentProxy(TypeElement typeElement) {
        return processorManager.parse(typeElement).map(this::buildComponentProxy).orElseThrow(() -> new InjectionProcessException(CANNOT_GET_COMPILATION_UNIT.bind(typeElement.getQualifiedName().toString())));
    }

    private CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit) {
        return buildComponentProxy(componentCompilationUnit, null);
    }

    private CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit, AnnotationExpr named) {
        return buildComponentProxy(
                componentCompilationUnit,
                processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit),
                named
        );
    }

    private CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, AnnotationExpr named) {
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .addModifier(Modifier.Keyword.PUBLIC)
                .addExtendedType(componentClassDeclaration.getNameAsString())
                .setName(componentClassDeclaration.getNameAsString().concat("Proxy"))
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

        componentClassDeclaration.getConstructors().forEach(
                constructorDeclaration -> {
                    ConstructorDeclaration constructorDeclarationClone = constructorDeclaration.clone();
                    constructorDeclarationClone.setParentNode(componentProxyClassDeclaration);
                    ConstructorDeclaration componentProxyClassConstructor = componentProxyClassDeclaration
                            .addConstructor(Modifier.Keyword.PUBLIC)
                            .setAnnotations(constructorDeclarationClone.getAnnotations())
                            .setParameters(constructorDeclarationClone.getParameters());

                    componentProxyClassConstructor
                            .createBody()
                            .addStatement(
                                    new MethodCallExpr()
                                            .setName(new SuperExpr().toString())
                                            .setArguments(
                                                    constructorDeclarationClone.getParameters().stream()
                                                            .map(NodeWithSimpleName::getNameAsExpression)
                                                            .collect(Collectors.toCollection(NodeList::new))
                                            )
                            );
                }
        );

        CompilationUnit componentProxyCompilationUnit = new CompilationUnit().addType(componentProxyClassDeclaration).addImport(Generated.class);

        componentClassDeclaration.getAnnotationByClass(Named.class)
                .ifPresent(annotationExpr -> {
                            AnnotationExpr annotationExprClone = annotationExpr.clone();
                            annotationExprClone.setParentNode(componentProxyClassDeclaration);
                            componentProxyClassDeclaration.addAnnotation(annotationExprClone);
                            componentProxyCompilationUnit.addImport(Named.class);
                        }
                );

        if (named != null) {
            AnnotationExpr annotationExprClone = named.clone();
            componentProxyClassDeclaration.addAnnotation(annotationExprClone);
            componentProxyCompilationUnit.addImport(Named.class);
        }
        componentCompilationUnit.getPackageDeclaration()
                .ifPresent(packageDeclaration -> componentProxyCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));

        processorManager.importAllClassOrInterfaceType(componentProxyClassDeclaration, componentClassDeclaration);

        componentProxyProcessors
                .forEach(componentProxyProcessor -> {
                            Logger.debug("processComponentProxy {}", componentProxyProcessor.getClass().getName());
                            componentProxyProcessor.processComponentProxy(componentCompilationUnit, componentClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration);
                        }
                );
        Logger.info("{} proxy class build success", processorManager.getQualifiedNameByDeclaration(componentClassDeclaration));
        return componentProxyCompilationUnit;
    }

    private Stream<CompilationUnit> buildProducesComponentProxyStream(TypeElement typeElement) {
        return processorManager.parse(typeElement).map(this::buildProducesComponentProxyStream).orElseThrow(() -> new InjectionProcessException(CANNOT_GET_COMPILATION_UNIT.bind(typeElement.getQualifiedName().toString())));
    }

    private Stream<CompilationUnit> buildProducesComponentProxyStream(CompilationUnit componentCompilationUnit) {
        return buildProducesComponentProxyStream(
                processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit)
        );
    }

    private Stream<CompilationUnit> buildProducesComponentProxyStream(ClassOrInterfaceDeclaration componentClassDeclaration) {
        return componentClassDeclaration.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                .flatMap(methodDeclaration -> {
                            if (methodDeclaration.getType().isClassOrInterfaceType() && processorManager.getQualifiedNameByType(methodDeclaration.getType()).equals(PublisherBuilder.class.getName())) {
                                return processorManager.getMethodReturnReferenceType(methodDeclaration)
                                        .map(resolvedReferenceType -> resolvedReferenceType.getTypeParametersMap().get(0).b.asReferenceType())
                                        .map(resolvedReferenceType -> processorManager.getCompilationUnitByResolvedReferenceType(resolvedReferenceType))
                                        .map(compilationUnit -> buildComponentProxy(compilationUnit, methodDeclaration.getAnnotationByClass(Named.class).orElse(null)));

                            } else {
                                return processorManager.getMethodReturnReferenceType(methodDeclaration)
                                        .map(resolvedReferenceType -> processorManager.getCompilationUnitByResolvedReferenceType(resolvedReferenceType))
                                        .map(compilationUnit -> buildComponentProxy(compilationUnit, methodDeclaration.getAnnotationByClass(Named.class).orElse(null)));
                            }
                        }
                );
    }

    private CompilationUnit buildModule(Set<? extends Element> singletonSet,
                                        Set<? extends Element> dependentSet,
                                        Set<? extends Element> applicationScopedSet,
                                        Set<? extends Element> requestScopedSet,
                                        Set<? extends Element> sessionScopedSet,
                                        List<CompilationUnit> componentProxyCompilationUnits) {

        ClassOrInterfaceDeclaration moduleClassDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName("PackageModule" + round.get())
                .addAnnotation(Module.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

        CompilationUnit moduleCompilationUnit = new CompilationUnit().addType(moduleClassDeclaration)
                .setPackageDeclaration(processorManager.getRootPackageName())
                .addImport(Module.class)
                .addImport(Provides.class)
                .addImport(javax.inject.Singleton.class)
                .addImport(Generated.class)
                .addImport("io.graphoenix.core.context.BeanContext");

        Streams.concat(singletonSet.stream(), applicationScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> processorManager.getCompilationUnitBySourceCode(typeElement))
                .forEach(componentCompilationUnit -> buildProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, true, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        requestScopedSet.stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> processorManager.getCompilationUnitBySourceCode(typeElement))
                .forEach(componentCompilationUnit -> buildRequestScopeProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        sessionScopedSet.stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> processorManager.getCompilationUnitBySourceCode(typeElement))
                .forEach(componentCompilationUnit -> buildSessionScopeProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        dependentSet.stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> processorManager.getCompilationUnitBySourceCode(typeElement))
                .forEach(componentCompilationUnit -> buildProvidesMethod(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, false, getComponentProxyCompilationUnit(componentCompilationUnit, componentProxyCompilationUnits)));

        componentProxyProcessors
                .forEach(componentProxyProcessor -> {
                            Logger.debug("processModule {}", componentProxyProcessor.getClass().getName());
                            componentProxyProcessor.processModule(moduleCompilationUnit, moduleClassDeclaration);
                        }
                );
        Logger.info("{} package module class build success", processorManager.getQualifiedNameByDeclaration(moduleClassDeclaration));
        return moduleCompilationUnit;
    }

    private void buildProvidesMethod(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, boolean isSingleton, CompilationUnit componentProxyCompilationUnit) {
        ClassOrInterfaceDeclaration componentClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit);
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit);

        moduleCompilationUnit.addImport(processorManager.getNameByDeclaration(componentProxyClassDeclaration));

        MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, componentClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                .addAnnotation(Provides.class)
                .setType(componentClassDeclaration.getNameAsString());
        if (isSingleton) {
            methodDeclaration.addAnnotation(javax.inject.Singleton.class);
        }

        methodDeclaration.createBody().addStatement(buildProvidesMethodReturnStmt(moduleCompilationUnit, componentProxyClassDeclaration));
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentClassDeclaration);
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentProxyClassDeclaration);
    }

    private void buildRequestScopeProvidesMethod(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, CompilationUnit componentProxyCompilationUnit) {
        ClassOrInterfaceDeclaration componentClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit);
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit);

        moduleCompilationUnit.addImport(processorManager.getNameByDeclaration(componentProxyClassDeclaration));

        MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, componentClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                .addAnnotation(Provides.class)
                .setType(new ClassOrInterfaceType().setName(PublisherBuilder.class.getSimpleName()).setTypeArguments(new ClassOrInterfaceType().setName(componentClassDeclaration.getName())));

        methodDeclaration.createBody().addStatement(buildRequestScopeProvidesMethodReturnStmt(moduleCompilationUnit, processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit), componentProxyClassDeclaration));
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentClassDeclaration);
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentProxyClassDeclaration);
    }

    private void buildSessionScopeProvidesMethod(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, CompilationUnit componentProxyCompilationUnit) {
        ClassOrInterfaceDeclaration componentClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit);
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit);

        moduleCompilationUnit.addImport(processorManager.getNameByDeclaration(componentProxyClassDeclaration));

        MethodDeclaration methodDeclaration = moduleClassDeclaration.addMethod(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, componentClassDeclaration.getNameAsString()), Modifier.Keyword.PUBLIC)
                .addAnnotation(Provides.class)
                .setType(new ClassOrInterfaceType().setName(PublisherBuilder.class.getSimpleName()).setTypeArguments(new ClassOrInterfaceType().setName(componentClassDeclaration.getName())));

        methodDeclaration.createBody().addStatement(buildSessionScopeProvidesMethodReturnStmt(moduleCompilationUnit, processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit), componentProxyClassDeclaration));
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentClassDeclaration);
        processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, componentProxyClassDeclaration);
    }

    private ReturnStmt buildProvidesMethodReturnStmt(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        return new ReturnStmt(
                componentProxyClassDeclaration.getMembers().stream()
                        .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(Produces.class))
                        .filter(bodyDeclaration -> bodyDeclaration.isConstructorDeclaration() || bodyDeclaration.isMethodDeclaration())
                        .findFirst()
                        .map(bodyDeclaration -> {
                                    if (bodyDeclaration.isConstructorDeclaration()) {
                                        return new ObjectCreationExpr()
                                                .setType(componentProxyClassDeclaration.getNameAsString())
                                                .setArguments(
                                                        bodyDeclaration.asConstructorDeclaration().getParameters().stream()
                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                .collect(Collectors.toCollection(NodeList::new))
                                                );
                                    } else {
                                        moduleCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration));
                                        return new MethodCallExpr()
                                                .setName(bodyDeclaration.asMethodDeclaration().getNameAsString())
                                                .setArguments(
                                                        bodyDeclaration.asMethodDeclaration().getParameters().stream()
                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                .collect(Collectors.toCollection(NodeList::new))
                                                )
                                                .setScope(new NameExpr(componentProxyClassDeclaration.getNameAsString()));
                                    }
                                }
                        )
                        .orElseGet(() ->
                                new ObjectCreationExpr()
                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                        .setArguments(
                                                componentProxyClassDeclaration.getConstructors().stream()
                                                        .findFirst()
                                                        .map(constructorDeclaration ->
                                                                constructorDeclaration.getParameters().stream()
                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                        )
                                                        .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(componentProxyClassDeclaration.getNameAsString())))
                                        )
                        )
        );
    }

    private ReturnStmt buildRequestScopeProvidesMethodReturnStmt(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        moduleCompilationUnit.addImport(PublisherBuilder.class);
        moduleCompilationUnit.addImport("io.graphoenix.core.context.RequestPublisherBuilderFactory");
        return new ReturnStmt(
                componentProxyClassDeclaration.getMembers().stream()
                        .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(Produces.class))
                        .filter(bodyDeclaration -> bodyDeclaration.isConstructorDeclaration() || bodyDeclaration.isMethodDeclaration())
                        .findFirst()
                        .map(bodyDeclaration -> {
                                    if (bodyDeclaration.isConstructorDeclaration()) {
                                        return new MethodCallExpr()
                                                .setName("getPublisherBuilder")
                                                .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                                .addArgument(
                                                        new ObjectCreationExpr()
                                                                .setType(componentProxyClassDeclaration.getNameAsString())
                                                                .setArguments(
                                                                        bodyDeclaration.asConstructorDeclaration().getParameters().stream()
                                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                .collect(Collectors.toCollection(NodeList::new))
                                                                )
                                                )
                                                .setScope(new NameExpr("RequestPublisherBuilderFactory"));
                                    } else {
                                        moduleCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration));
                                        return new MethodCallExpr()
                                                .setName("getPublisherBuilder")
                                                .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                                .addArgument(
                                                        new MethodCallExpr()
                                                                .setName(bodyDeclaration.asMethodDeclaration().getNameAsString())
                                                                .setArguments(
                                                                        bodyDeclaration.asMethodDeclaration().getParameters().stream()
                                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                .collect(Collectors.toCollection(NodeList::new))
                                                                )
                                                                .setScope(new NameExpr(componentProxyClassDeclaration.getNameAsString()))
                                                )
                                                .setScope(new NameExpr("RequestPublisherBuilderFactory"));
                                    }
                                }
                        )
                        .orElseGet(() ->
                                new MethodCallExpr()
                                        .setName("getPublisherBuilder")
                                        .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                        .addArgument(
                                                new ObjectCreationExpr()
                                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                                        .setArguments(
                                                                componentProxyClassDeclaration.getConstructors().stream()
                                                                        .findFirst()
                                                                        .map(constructorDeclaration ->
                                                                                constructorDeclaration.getParameters().stream()
                                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                                        )
                                                                        .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(componentProxyClassDeclaration.getNameAsString())))
                                                        )
                                        )
                                        .setScope(new NameExpr("RequestPublisherBuilderFactory"))
                        )
        );
    }

    private ReturnStmt buildSessionScopeProvidesMethodReturnStmt(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {
        moduleCompilationUnit.addImport(PublisherBuilder.class);
        moduleCompilationUnit.addImport("io.graphoenix.core.context.SessionPublisherBuilderFactory");
        return new ReturnStmt(
                componentProxyClassDeclaration.getMembers().stream()
                        .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(Produces.class))
                        .filter(bodyDeclaration -> bodyDeclaration.isConstructorDeclaration() || bodyDeclaration.isMethodDeclaration())
                        .findFirst()
                        .map(bodyDeclaration -> {
                                    if (bodyDeclaration.isConstructorDeclaration()) {
                                        return new MethodCallExpr()
                                                .setName("getPublisherBuilder")
                                                .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                                .addArgument(
                                                        new ObjectCreationExpr()
                                                                .setType(componentProxyClassDeclaration.getNameAsString())
                                                                .setArguments(
                                                                        bodyDeclaration.asConstructorDeclaration().getParameters().stream()
                                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                .collect(Collectors.toCollection(NodeList::new))
                                                                )
                                                )
                                                .setScope(new NameExpr("SessionPublisherBuilderFactory"));
                                    } else {
                                        moduleCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration));
                                        return new MethodCallExpr()
                                                .setName("getPublisherBuilder")
                                                .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                                .addArgument(
                                                        new MethodCallExpr()
                                                                .setName(bodyDeclaration.asMethodDeclaration().getNameAsString())
                                                                .setArguments(
                                                                        bodyDeclaration.asMethodDeclaration().getParameters().stream()
                                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                .collect(Collectors.toCollection(NodeList::new))
                                                                )
                                                                .setScope(new NameExpr(componentProxyClassDeclaration.getNameAsString()))
                                                )
                                                .setScope(new NameExpr("SessionPublisherBuilderFactory"));
                                    }
                                }
                        )
                        .orElseGet(() ->
                                new MethodCallExpr()
                                        .setName("getPublisherBuilder")
                                        .addArgument(new ClassExpr().setType(componentClassDeclaration.getNameAsString()))
                                        .addArgument(
                                                new ObjectCreationExpr()
                                                        .setType(componentProxyClassDeclaration.getNameAsString())
                                                        .setArguments(
                                                                componentProxyClassDeclaration.getConstructors().stream()
                                                                        .findFirst()
                                                                        .map(constructorDeclaration ->
                                                                                constructorDeclaration.getParameters().stream()
                                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                                        )
                                                                        .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(componentProxyClassDeclaration.getNameAsString())))
                                                        )
                                        )
                                        .setScope(new NameExpr("SessionPublisherBuilderFactory"))
                        )
        );
    }

    private MethodCallExpr getBeanGetMethodCallExpr(NodeWithAnnotations<?> nodeWithAnnotations, CompilationUnit belongCompilationUnit, ClassOrInterfaceType classOrInterfaceType) {
        Optional<StringLiteralExpr> nameStringExpr = nodeWithAnnotations.getAnnotationByClass(Named.class)
                .map(Expression::asNormalAnnotationExpr)
                .flatMap(normalAnnotationExpr ->
                        normalAnnotationExpr.getPairs().stream()
                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                .findFirst()
                );
        MethodCallExpr methodCallExpr;
        if (processorManager.getQualifiedNameByType(classOrInterfaceType).equals(Provider.class.getName())) {
            Type type = classOrInterfaceType.getTypeArguments().orElseThrow(() -> new InjectionProcessException(PROVIDER_TYPE_NOT_EXIST)).get(0);
            if (type.isClassOrInterfaceType() && processorManager.getQualifiedNameByType(type).equals(PublisherBuilder.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getPublisherBuilderProvider")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(type.asClassOrInterfaceType().getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0)));

            } else {
                methodCallExpr = new MethodCallExpr()
                        .setName("getProvider")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(type));
            }
            belongCompilationUnit.addImport(Provider.class);
        } else {
            if (processorManager.getQualifiedNameByType(classOrInterfaceType).equals(PublisherBuilder.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getPublisherBuilder")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(classOrInterfaceType.getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0)));
            } else {
                methodCallExpr = new MethodCallExpr()
                        .setName("get")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(classOrInterfaceType));
            }
        }
        nameStringExpr.ifPresent(methodCallExpr::addArgument);
        return methodCallExpr;
    }

    private CompilationUnit getComponentProxyCompilationUnit(CompilationUnit componentCompilationUnit, List<CompilationUnit> componentProxyCompilationUnits) {
        return componentProxyCompilationUnits.stream()
                .filter(componentProxyCompilationUnit ->
                        processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit).getExtendedTypes().stream()
                                .anyMatch(classOrInterfaceType -> classOrInterfaceType.getNameAsString().equals(processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).getNameAsString()))
                )
                .findFirst()
                .orElseThrow(() -> new InjectionProcessException(CANNOT_GET_COMPILATION_UNIT.bind(processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit).getNameAsString())));
    }

    private Stream<CompilationUnit> buildProducesModuleStream(Set<? extends Element> singletonSet,
                                                              Set<? extends Element> dependentSet,
                                                              Set<? extends Element> applicationScopedSet,
                                                              Set<? extends Element> requestScopedSet,
                                                              Set<? extends Element> sessionScopedSet,
                                                              List<CompilationUnit> producesComponentProxyCompilationUnits) {

        return Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream(), requestScopedSet.stream(), sessionScopedSet.stream())
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().isClass())
                .map(element -> (TypeElement) element)
                .map(typeElement -> processorManager.getCompilationUnitBySourceCode(typeElement))
                .filter(compilationUnit ->
                        processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit).getMethods().stream()
                                .anyMatch(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                )
                .map(compilationUnit -> {
                            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(compilationUnit);
                            ClassOrInterfaceDeclaration moduleClassDeclaration = new ClassOrInterfaceDeclaration()
                                    .setPublic(true)
                                    .setName(classOrInterfaceDeclaration.getNameAsString().concat("Module"))
                                    .addAnnotation(Module.class)
                                    .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()));

                            CompilationUnit moduleCompilationUnit = new CompilationUnit().addType(moduleClassDeclaration)
                                    .addImport(Module.class)
                                    .addImport(Provides.class)
                                    .addImport(javax.inject.Singleton.class)
                                    .addImport(Generated.class)
                                    .addImport("io.graphoenix.core.context.BeanContext");

                            NodeList<ClassOrInterfaceType> extendedTypes = classOrInterfaceDeclaration.getExtendedTypes().stream()
                                    .map(classOrInterfaceType -> {
                                                ClassOrInterfaceType classOrInterfaceTypeClone = classOrInterfaceType.clone();
                                                classOrInterfaceTypeClone.setParentNode(moduleClassDeclaration);
                                                return classOrInterfaceTypeClone;
                                            }
                                    )
                                    .collect(Collectors.toCollection(NodeList::new));

                            NodeList<ClassOrInterfaceType> implementedTypes = classOrInterfaceDeclaration.getImplementedTypes().stream()
                                    .map(classOrInterfaceType -> {
                                                ClassOrInterfaceType classOrInterfaceTypeClone = classOrInterfaceType.clone();
                                                classOrInterfaceTypeClone.setParentNode(moduleClassDeclaration);
                                                return classOrInterfaceTypeClone;
                                            }
                                    )
                                    .collect(Collectors.toCollection(NodeList::new));

                            NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers().stream()
                                    .map(bodyDeclaration -> {
                                                BodyDeclaration<?> BodyDeclarationClone = bodyDeclaration.clone();
                                                BodyDeclarationClone.setParentNode(moduleClassDeclaration);
                                                return BodyDeclarationClone;
                                            }
                                    )
                                    .collect(Collectors.toCollection(NodeList::new));

                            compilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> moduleCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));
                            compilationUnit.getImports().forEach(importDeclaration -> moduleCompilationUnit.addImport(importDeclaration.getNameAsString()));

                            moduleClassDeclaration.setExtendedTypes(extendedTypes);
                            moduleClassDeclaration.setImplementedTypes(implementedTypes);
                            moduleClassDeclaration.setMembers(members);
                            moduleClassDeclaration.getConstructors().forEach(constructorDeclaration -> constructorDeclaration.setName(moduleClassDeclaration.getNameAsString()));

                            moduleClassDeclaration.getFields().stream()
                                    .filter(fieldDeclaration -> fieldDeclaration.isAnnotationPresent(Inject.class))
                                    .forEach(fieldDeclaration -> {
                                                List<VariableDeclarator> removeVariableDeclaratorList = new ArrayList<>();
                                                fieldDeclaration.getVariables()
                                                        .forEach(variableDeclarator -> {
                                                                    if (variableDeclarator.getType().isClassOrInterfaceType()) {
                                                                        moduleClassDeclaration.getMethods().stream()
                                                                                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                                                                                .forEach(methodDeclaration -> {
                                                                                            methodDeclaration.findAll(FieldAccessExpr.class).stream()
                                                                                                    .filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr() || fieldAccessExpr.getScope() == null)
                                                                                                    .filter(fieldAccessExpr -> fieldAccessExpr.getNameAsString().equals(variableDeclarator.getNameAsString()))
                                                                                                    .forEach(fieldAccessExpr ->
                                                                                                            fieldAccessExpr.getParentNode()
                                                                                                                    .ifPresent(node -> {
                                                                                                                                node.replace(
                                                                                                                                        fieldAccessExpr,
                                                                                                                                        getBeanGetMethodCallExpr(fieldDeclaration, moduleCompilationUnit, variableDeclarator.getType().asClassOrInterfaceType())
                                                                                                                                );
                                                                                                                                removeVariableDeclaratorList.add(variableDeclarator);
                                                                                                                            }
                                                                                                                    )
                                                                                                    );
                                                                                            methodDeclaration.findAll(NameExpr.class).stream()
                                                                                                    .filter(nameExpr -> nameExpr.getNameAsString().equals(variableDeclarator.getNameAsString()))
                                                                                                    .forEach(nameExpr ->
                                                                                                            nameExpr.getParentNode()
                                                                                                                    .ifPresent(node -> {
                                                                                                                                node.replace(
                                                                                                                                        nameExpr,
                                                                                                                                        getBeanGetMethodCallExpr(fieldDeclaration, moduleCompilationUnit, variableDeclarator.getType().asClassOrInterfaceType())
                                                                                                                                );
                                                                                                                                removeVariableDeclaratorList.add(variableDeclarator);
                                                                                                                            }
                                                                                                                    )
                                                                                                    );
                                                                                        }
                                                                                );
                                                                    }
                                                                }
                                                        );
                                                fieldDeclaration.getVariables().removeAll(removeVariableDeclaratorList);
                                                fieldDeclaration.getAnnotationByClass(Inject.class).ifPresent(Node::remove);
                                                if (fieldDeclaration.getVariables().size() == 0) {
                                                    fieldDeclaration.remove();
                                                }
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
                                                                            .filter(parameter -> assignExpr.getValue().isNameExpr())
                                                                            .filter(parameter -> parameter.getNameAsString().equals(assignExpr.getValue().asNameExpr().getNameAsString()))
                                                                            .forEach(parameter ->
                                                                                    moduleClassDeclaration.getMethods().stream()
                                                                                            .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                                                                                            .forEach(methodDeclaration -> {
                                                                                                        methodDeclaration.findAll(FieldAccessExpr.class).stream()
                                                                                                                .filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr() || fieldAccessExpr.getScope() == null)
                                                                                                                .filter(fieldAccessExpr -> fieldAccessExpr.getNameAsString().equals(assignExpr.getTarget().asFieldAccessExpr().getNameAsString()))
                                                                                                                .forEach(fieldAccessExpr -> {
                                                                                                                            fieldAccessExpr.getParentNode()
                                                                                                                                    .ifPresent(node ->
                                                                                                                                            node.replace(
                                                                                                                                                    fieldAccessExpr,
                                                                                                                                                    getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType())
                                                                                                                                            )
                                                                                                                                    );
                                                                                                                            moduleClassDeclaration.getFields().stream().flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                                                                                                                                    .filter(variableDeclarator -> assignExpr.getTarget().asFieldAccessExpr().getNameAsString().equals(variableDeclarator.getNameAsString()))
                                                                                                                                    .findFirst()
                                                                                                                                    .ifPresent(Node::remove);
                                                                                                                        }
                                                                                                                );
                                                                                                        methodDeclaration.findAll(NameExpr.class).stream()
                                                                                                                .filter(nameExpr -> nameExpr.getNameAsString().equals(assignExpr.getTarget().asFieldAccessExpr().getNameAsString()))
                                                                                                                .forEach(nameExpr -> {
                                                                                                                            nameExpr.getParentNode()
                                                                                                                                    .ifPresent(node ->
                                                                                                                                            node.replace(
                                                                                                                                                    nameExpr,
                                                                                                                                                    getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType())
                                                                                                                                            )
                                                                                                                                    );
                                                                                                                            moduleClassDeclaration.getFields().stream().flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                                                                                                                                    .filter(variableDeclarator -> assignExpr.getTarget().asFieldAccessExpr().getNameAsString().equals(variableDeclarator.getNameAsString()))
                                                                                                                                    .findFirst()
                                                                                                                                    .ifPresent(Node::remove);
                                                                                                                        }
                                                                                                                );
                                                                                                    }
                                                                                            )

                                                                            );
                                                                    constructorDeclaration.getAnnotationByClass(Inject.class).ifPresent(Node::remove);
                                                                }
                                                        );
                                                constructorDeclaration.getParameters().clear();
                                                constructorDeclaration.getBody().getStatements().clear();
                                                moduleClassDeclaration.getFields().stream()
                                                        .filter(fieldDeclaration -> fieldDeclaration.getVariables().size() == 0)
                                                        .forEach(Node::remove);
                                            }
                                    );

                            moduleClassDeclaration.getMethods().stream()
                                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                                    .forEach(producesMethodDeclaration -> {
                                                Type originalType = producesMethodDeclaration.getType().clone();
                                                producesMethodDeclaration.addAnnotation(Provides.class);
                                                if (producesMethodDeclaration.isAnnotationPresent(ApplicationScoped.class)) {
                                                    producesMethodDeclaration.addAnnotation(javax.inject.Singleton.class);
                                                }
                                                if (producesMethodDeclaration.isAnnotationPresent(Singleton.class)) {
                                                    producesMethodDeclaration.addAnnotation(javax.inject.Singleton.class);
                                                }

                                                producesMethodDeclaration.getBody()
                                                        .ifPresent(blockStmt ->
                                                                blockStmt.getStatements()
                                                                        .forEach(statement -> {
                                                                                    if (statement.isReturnStmt()) {
                                                                                        statement.asReturnStmt().getExpression()
                                                                                                .ifPresent(expression -> {
                                                                                                            if (expression.isObjectCreationExpr()) {
                                                                                                                ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                                                                                CompilationUnit componentCompilationUnit = processorManager.getCompilationUnitByClassOrInterfaceType(objectCreationExpr.getType());
                                                                                                                CompilationUnit componentProxyCompilationUnit = getComponentProxyCompilationUnit(componentCompilationUnit, producesComponentProxyCompilationUnits);
                                                                                                                ClassOrInterfaceDeclaration componentProxyClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit);
                                                                                                                objectCreationExpr.setType(componentProxyClassDeclaration.getNameAsString());
                                                                                                                moduleCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration));

                                                                                                                objectCreationExpr.getArguments().stream()
                                                                                                                        .filter(Expression::isMethodCallExpr)
                                                                                                                        .map(Expression::asMethodCallExpr)
                                                                                                                        .forEach(methodCallExpr -> replaceComponentMethod(objectCreationExpr.getArguments(), methodCallExpr, moduleClassDeclaration));

                                                                                                                objectCreationExpr.getArguments().stream()
                                                                                                                        .filter(Expression::isMethodReferenceExpr)
                                                                                                                        .map(Expression::asMethodReferenceExpr)
                                                                                                                        .forEach(methodReferenceExpr -> replaceComponentMethod(objectCreationExpr.getArguments(), methodReferenceExpr, moduleClassDeclaration));
                                                                                                            }
                                                                                                        }
                                                                                                );
                                                                                    }
                                                                                }
                                                                        )
                                                        );
                                                producesMethodDeclaration.getAnnotationByClass(Produces.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(ApplicationScoped.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(RequestScoped.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(SessionScoped.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(Singleton.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(Dependent.class).ifPresent(Node::remove);
                                                producesMethodDeclaration.getAnnotationByClass(Named.class).ifPresent(Node::remove);
                                            }
                                    );
                            processorManager.importAllClassOrInterfaceType(moduleClassDeclaration, classOrInterfaceDeclaration);
                            componentProxyProcessors
                                    .forEach(componentProxyProcessor -> {
                                                Logger.debug("processComponentModule {}", componentProxyProcessor.getClass().getName());
                                                componentProxyProcessor.processComponentModule(moduleCompilationUnit, moduleClassDeclaration);
                                            }
                                    );
                            Logger.info("{} module class build success", processorManager.getQualifiedNameByDeclaration(moduleClassDeclaration));
                            return moduleCompilationUnit;
                        }
                );
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
                    .ifPresent(methodDeclaration -> {
                                MethodCallExpr beanGetMethodCallExpr;
                                Type type = methodDeclaration.getType();
                                if (type.isClassOrInterfaceType() && processorManager.getQualifiedNameByType(type).equals(PublisherBuilder.class.getName())) {
                                    beanGetMethodCallExpr = new MethodCallExpr()
                                            .setName("getPublisherBuilder")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(type.asClassOrInterfaceType().getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0)));
                                } else {
                                    beanGetMethodCallExpr = new MethodCallExpr()
                                            .setName("get")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(type));
                                }

                                methodDeclaration.getAnnotationByClass(Named.class)
                                        .flatMap(annotationExpr ->
                                                annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                        .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                                        .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                                        .findFirst()
                                        )
                                        .ifPresent(beanGetMethodCallExpr::addArgument);

                                arguments.replace(methodCallExpr, beanGetMethodCallExpr);
                            }
                    );
        }
    }

    private void replaceComponentMethod(NodeList<Expression> arguments, MethodReferenceExpr methodReferenceExpr, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

        if (methodReferenceExpr.getScope().isThisExpr()) {
            classOrInterfaceDeclaration.getMethods().stream()
                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodReferenceExpr.getIdentifier()))
                    .findAny()
                    .ifPresent(methodDeclaration -> {
                                MethodCallExpr beanProviderGetMethodCallExpr;
                                Type type = methodDeclaration.getType();
                                if (type.isClassOrInterfaceType() && processorManager.getQualifiedNameByType(type).equals(PublisherBuilder.class.getName())) {
                                    beanProviderGetMethodCallExpr = new MethodCallExpr()
                                            .setName("getPublisherBuilderProvider")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(type.asClassOrInterfaceType().getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0)));
                                } else {
                                    beanProviderGetMethodCallExpr = new MethodCallExpr()
                                            .setName("getProvider")
                                            .setScope(new NameExpr().setName("BeanContext"))
                                            .addArgument(new ClassExpr().setType(type));
                                }

                                methodDeclaration.getAnnotationByClass(Named.class)
                                        .flatMap(annotationExpr ->
                                                annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                        .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                                        .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                                        .findFirst()
                                        )
                                        .ifPresent(beanProviderGetMethodCallExpr::addArgument);

                                arguments.replace(
                                        methodReferenceExpr,
                                        beanProviderGetMethodCallExpr
                                );
                            }
                    );
        }
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, CompilationUnit moduleCompilationUnit) {
        return buildComponentProxyComponent(
                componentProxyCompilationUnit,
                processorManager.getPublicClassOrInterfaceDeclaration(componentProxyCompilationUnit),
                processorManager.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit)
        );
    }

    private CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentProxyClassDeclaration.getNameAsString() + "_Component")
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        MethodDeclaration providesMethodDeclaration = moduleClassDeclaration.getMethods().stream()
                .filter(methodDeclaration -> {
                            if (methodDeclaration.getType().isClassOrInterfaceType() && processorManager.getQualifiedNameByType(methodDeclaration.getType()).equals(PublisherBuilder.class.getName())) {
                                return processorManager.getMethodReturnReferenceType(methodDeclaration)
                                        .map(resolvedReferenceType -> resolvedReferenceType.getTypeParametersMap().get(0).b.asReferenceType())
                                        .anyMatch(resolvedReferenceType ->
                                                resolvedReferenceType.getQualifiedName().equals(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration)) ||
                                                        componentProxyClassDeclaration.getExtendedTypes().stream().anyMatch(extendType -> processorManager.getQualifiedNameByType(extendType).equals(resolvedReferenceType.getQualifiedName())));

                            } else {
                                return processorManager.getMethodReturnReferenceType(methodDeclaration)
                                        .anyMatch(resolvedReferenceType ->
                                                resolvedReferenceType.getQualifiedName().equals(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration)) ||
                                                        componentProxyClassDeclaration.getExtendedTypes().stream().anyMatch(extendType -> processorManager.getQualifiedNameByType(extendType).equals(resolvedReferenceType.getQualifiedName())));
                            }
                        }
                )
                .findFirst()
                .orElseThrow(() -> new InjectionProcessException(MODULE_PROVIDERS_METHOD_NOT_EXIST.bind(processorManager.getQualifiedNameByDeclaration(componentProxyClassDeclaration))));

        Type typeClone = providesMethodDeclaration.getType().clone();

        if (providesMethodDeclaration.isAnnotationPresent(javax.inject.Singleton.class)) {
            componentProxyComponentInterfaceDeclaration.addAnnotation(javax.inject.Singleton.class);
        }

        typeClone.setParentNode(componentProxyComponentInterfaceDeclaration);

        componentProxyComponentInterfaceDeclaration.addMethod("get").setType(typeClone).removeBody();

        CompilationUnit componentProxyComponentCompilationUnit = new CompilationUnit()
                .addType(componentProxyComponentInterfaceDeclaration)
                .addImport(Component.class);

        componentProxyClassDeclaration.getAnnotationByClass(Named.class)
                .ifPresent(annotationExpr -> {
                            AnnotationExpr AnnotationExprClone = annotationExpr.clone();
                            AnnotationExprClone.setParentNode(componentProxyClassDeclaration);
                            componentProxyComponentInterfaceDeclaration.addAnnotation(AnnotationExprClone);
                            componentProxyComponentCompilationUnit.addImport(Named.class);
                        }
                );

        componentProxyCompilationUnit.getPackageDeclaration()
                .ifPresent(packageDeclaration -> componentProxyComponentCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));

        componentProxyComponentCompilationUnit.addImport(processorManager.getQualifiedNameByDeclaration(moduleClassDeclaration));

        processorManager.importAllClassOrInterfaceType(componentProxyComponentInterfaceDeclaration, componentProxyClassDeclaration);
        processorManager.importAllClassOrInterfaceType(componentProxyComponentInterfaceDeclaration, moduleClassDeclaration);
        Logger.info("{} component class build success", processorManager.getQualifiedNameByDeclaration(componentProxyComponentInterfaceDeclaration));
        return componentProxyComponentCompilationUnit;
    }

    private CompilationUnit buildModuleContext(List<CompilationUnit> componentProxyComponentCompilationUnits, CompilationUnit moduleCompilationUnit) {
        return buildModuleContext(componentProxyComponentCompilationUnits, moduleCompilationUnit, processorManager.getPublicClassOrInterfaceDeclaration(moduleCompilationUnit));
    }

    private CompilationUnit buildModuleContext(List<CompilationUnit> componentProxyComponentCompilationUnits, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleClassDeclaration.getNameAsString().concat("_Context"))
                .addAnnotation(
                        new SingleMemberAnnotationExpr()
                                .setMemberValue(new ClassExpr().setType(ModuleContext.class))
                                .setName(AutoService.class.getSimpleName())
                )
                .addExtendedType(BaseModuleContext.class);

        CompilationUnit moduleContextCompilationUnit = new CompilationUnit()
                .addType(moduleContextInterfaceDeclaration)
                .addImport(AutoService.class)
                .addImport(ModuleContext.class)
                .addImport(BaseModuleContext.class);

        moduleCompilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> moduleContextCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));

        BlockStmt blockStmt = moduleContextInterfaceDeclaration.addStaticInitializer();

        componentProxyComponentCompilationUnits.forEach(
                componentProxyComponentCompilationUnit -> {
                    ClassOrInterfaceDeclaration componentProxyComponentClassDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentProxyComponentCompilationUnit);
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
                            .orElseThrow(() -> new InjectionProcessException(COMPONENT_GET_METHOD_NOT_EXIST.bind(componentProxyComponentClassDeclaration.getNameAsString())));

                    if (processorManager.getQualifiedNameByType(componentType).equals(PublisherBuilder.class.getName())) {
                        Optional<NodeList<Type>> typeArguments = componentType.getTypeArguments();
                        if (typeArguments.isPresent()) {
                            if (typeArguments.get().get(0) != null && typeArguments.get().get(0).isClassOrInterfaceType()) {
                                componentType = typeArguments.get().get(0).asClassOrInterfaceType();
                            }
                        }
                    }
                    moduleContextCompilationUnit.addImport(processorManager.getQualifiedNameByType(componentType));

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
                    Optional<StringLiteralExpr> nameStringExpr = componentProxyComponentClassDeclaration.getAnnotationByClass(Named.class)
                            .flatMap(annotationExpr ->
                                    annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                            .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                            .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr())
                                            .findFirst()
                            );

                    addPutTypeStatement(blockStmt, componentType, nameStringExpr.orElse(null), daggerVariableName);

                    componentProxyComponentCompilationUnit.getPackageDeclaration()
                            .ifPresent(packageDeclaration -> {
                                        moduleContextCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(componentProxyComponentClassDeclaration.getNameAsString()));
                                        moduleContextCompilationUnit.addImport(packageDeclaration.getNameAsString().concat(".").concat(daggerClassName));
                                    }
                            );

                    CompilationUnit componentCompilationUnit = processorManager.getCompilationUnitByClassOrInterfaceType(componentType);
                    ClassOrInterfaceDeclaration componentDeclaration = processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit);
                    componentDeclaration.getExtendedTypes()
                            .forEach(extendedType -> {
                                        addPutTypeStatement(blockStmt, extendedType, nameStringExpr.orElse(null), daggerVariableName);
                                        moduleContextCompilationUnit.addImport(processorManager.getQualifiedNameByType(extendedType));
                                    }
                            );
                    componentDeclaration.getImplementedTypes()
                            .forEach(implementedType -> {
                                        addPutTypeStatement(blockStmt, implementedType, nameStringExpr.orElse(null), daggerVariableName);
                                        moduleContextCompilationUnit.addImport(processorManager.getQualifiedNameByType(implementedType));
                                    }
                            );
                }
        );
        componentProxyProcessors.forEach(componentProxyProcessor -> {
                    Logger.debug("processModuleContext {}", componentProxyProcessor.getClass().getName());
                    componentProxyProcessor.processModuleContext(moduleContextCompilationUnit, blockStmt);
                }
        );
        Logger.info("{} module context class build success", processorManager.getQualifiedNameByDeclaration(moduleContextInterfaceDeclaration));
        return moduleContextCompilationUnit;
    }

    private void addPutTypeStatement(BlockStmt blockStmt, ClassOrInterfaceType classOrInterfaceType, StringLiteralExpr nameStringExpr, String daggerVariableName) {
        if (nameStringExpr != null) {
            blockStmt.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(classOrInterfaceType.getNameAsString()))
                            .addArgument(nameStringExpr)
                            .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
            );
        } else {
            blockStmt.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(classOrInterfaceType.getNameAsString()))
                            .addArgument(new MethodReferenceExpr().setIdentifier("get").setScope(new NameExpr().setName(daggerVariableName)))
            );
        }
    }
}
