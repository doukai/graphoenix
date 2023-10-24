package io.graphoenix.inject;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.google.common.collect.Streams;
import io.graphoenix.inject.error.InjectionProcessException;
import io.graphoenix.spi.context.BaseModuleContext;
import io.graphoenix.spi.context.ModuleContext;
import jakarta.annotation.Generated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionScoped;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.inject.error.InjectionProcessErrorType.CANNOT_GET_COMPILATION_UNIT;
import static io.graphoenix.inject.error.InjectionProcessErrorType.CONSTRUCTOR_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.INSTANCE_TYPE_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionProcessErrorType.PROVIDER_TYPE_NOT_EXIST;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes({
        "jakarta.inject.Singleton",
        "jakarta.enterprise.context.Dependent",
        "jakarta.enterprise.context.ApplicationScoped",
        "jakarta.enterprise.context.RequestScoped",
        "jakarta.enterprise.context.SessionScoped",
        "jakarta.transaction.TransactionScoped",
        "org.eclipse.microprofile.config.inject.ConfigProperties"
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
        Set<? extends Element> transactionScopedSet = roundEnv.getElementsAnnotatedWith(TransactionScoped.class);
        Set<? extends Element> configPropertiesSet = roundEnv.getElementsAnnotatedWith(ConfigProperties.class);

        List<TypeElement> typeElements = Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream(), requestScopedSet.stream(), sessionScopedSet.stream(), transactionScopedSet.stream(), configPropertiesSet.stream())
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
                .filter(typeElement -> typeElement.getAnnotation(ConfigProperties.class) == null)
                .map(this::buildComponentProxy)
                .collect(Collectors.toList());
        componentProxyCompilationUnits.forEach(compilationUnit -> processorManager.writeToFiler(compilationUnit));
        Logger.debug("all proxy class build success");

        CompilationUnit moduleContextCompilationUnit = buildModuleContext(typeElements.stream().flatMap(typeElement -> processorManager.parse(typeElement).stream()).collect(Collectors.toList()));
        processorManager.writeToFiler(moduleContextCompilationUnit);
        Logger.debug("module context class build success");

        List<CompilationUnit> producesContextCompilationUnits = buildProducesContextStream(singletonSet, dependentSet, applicationScopedSet, requestScopedSet, sessionScopedSet, transactionScopedSet, configPropertiesSet).collect(Collectors.toList());
        producesContextCompilationUnits.forEach(producesModuleCompilationUnit -> {
                    processorManager.writeToFiler(producesModuleCompilationUnit);
                    Logger.debug("produces context class build success");
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
        return buildComponentProxy(componentCompilationUnit, null, null);
    }

    private CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit, AnnotationExpr named, AnnotationExpr defaultAnnotation) {
        return buildComponentProxy(
                componentCompilationUnit,
                processorManager.getPublicClassOrInterfaceDeclaration(componentCompilationUnit),
                named,
                defaultAnnotation
        );
    }

    private CompilationUnit buildComponentProxy(CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, AnnotationExpr named, AnnotationExpr defaultAnnotation) {
        ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .addModifier(Modifier.Keyword.PUBLIC)
                .addExtendedType(componentClassDeclaration.getNameAsString())
                .setName(componentClassDeclaration.getNameAsString() + "Proxy")
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

        componentClassDeclaration.getAnnotationByClass(Default.class)
                .ifPresent(annotationExpr -> {
                            AnnotationExpr annotationExprClone = annotationExpr.clone();
                            annotationExprClone.setParentNode(componentProxyClassDeclaration);
                            componentProxyClassDeclaration.addAnnotation(annotationExprClone);
                            componentProxyCompilationUnit.addImport(Default.class);
                        }
                );

        if (named != null) {
            AnnotationExpr annotationExprClone = named.clone();
            componentProxyClassDeclaration.addAnnotation(annotationExprClone);
            componentProxyCompilationUnit.addImport(Named.class);
        }
        if (defaultAnnotation != null) {
            AnnotationExpr annotationExprClone = defaultAnnotation.clone();
            componentProxyClassDeclaration.addAnnotation(annotationExprClone);
            componentProxyCompilationUnit.addImport(Default.class);
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

    private Expression buildScopeFactoryGetMethodExpression(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, String factoryName) {
        String qualifiedName = processorManager.getQualifiedNameByDeclaration(componentClassDeclaration);
        moduleCompilationUnit.addImport(Mono.class)
                .addImport(PublisherBuilder.class)
                .addImport("io.graphoenix.core.context." + factoryName);

        return componentClassDeclaration.getMembers().stream()
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(Produces.class))
                .filter(bodyDeclaration -> bodyDeclaration.isConstructorDeclaration() || bodyDeclaration.isMethodDeclaration())
                .findFirst()
                .map(bodyDeclaration -> {
                            if (bodyDeclaration.isConstructorDeclaration()) {
                                return new MethodCallExpr()
                                        .setName("get")
                                        .addArgument(new ClassExpr().setType(qualifiedName))
                                        .addArgument(
                                                new LambdaExpr()
                                                        .setEnclosingParameters(true)
                                                        .setBody(
                                                                new ExpressionStmt()
                                                                        .setExpression(
                                                                                new ObjectCreationExpr()
                                                                                        .setType(qualifiedName + "Proxy")
                                                                                        .setArguments(
                                                                                                bodyDeclaration.asConstructorDeclaration().getParameters().stream()
                                                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .setScope(new NameExpr(factoryName));
                            } else {
                                return new MethodCallExpr()
                                        .setName("get")
                                        .addArgument(new ClassExpr().setType(qualifiedName))
                                        .addArgument(
                                                new LambdaExpr()
                                                        .setEnclosingParameters(true)
                                                        .setBody(
                                                                new ExpressionStmt()
                                                                        .setExpression(
                                                                                new MethodCallExpr()
                                                                                        .setName(bodyDeclaration.asMethodDeclaration().getNameAsString())
                                                                                        .setArguments(
                                                                                                bodyDeclaration.asMethodDeclaration().getParameters().stream()
                                                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                                                        )
                                                                                        .setScope(new NameExpr(qualifiedName + "Proxy"))
                                                                        )
                                                        )
                                        )
                                        .setScope(new NameExpr(factoryName));
                            }
                        }
                )
                .orElseGet(() ->
                        new MethodCallExpr()
                                .setName("get")
                                .addArgument(new ClassExpr().setType(componentClassDeclaration.getFullyQualifiedName().orElseGet(componentClassDeclaration::getNameAsString)))
                                .addArgument(
                                        new LambdaExpr()
                                                .setEnclosingParameters(true)
                                                .setBody(
                                                        new ExpressionStmt()
                                                                .setExpression(
                                                                        new ObjectCreationExpr()
                                                                                .setType(qualifiedName + "Proxy")
                                                                                .setArguments(
                                                                                        componentClassDeclaration.getConstructors().stream()
                                                                                                .findFirst()
                                                                                                .map(constructorDeclaration ->
                                                                                                        constructorDeclaration.getParameters().stream()
                                                                                                                .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                                                .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                                                .collect(Collectors.toCollection(NodeList::new))
                                                                                                )
                                                                                                .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(qualifiedName)))
                                                                                )
                                                                )
                                                )
                                )
                                .setScope(new NameExpr(factoryName))
                );
    }

    private MethodCallExpr getBeanGetMethodCallExpr(NodeWithAnnotations<?> nodeWithAnnotations, CompilationUnit belongCompilationUnit, ClassOrInterfaceType classOrInterfaceType) {
        Optional<StringLiteralExpr> nameStringExpr = nodeWithAnnotations.getAnnotationByClass(Default.class)
                .map(annotationExpr -> new StringLiteralExpr("default"))
                .or(() ->
                        nodeWithAnnotations.getAnnotationByClass(Named.class)
                                .flatMap(processorManager::findAnnotationValue)
                                .map(Expression::asStringLiteralExpr)
                );

        MethodCallExpr methodCallExpr;
        if (processorManager.getQualifiedNameByType(classOrInterfaceType).equals(Provider.class.getName())) {
            Type type = classOrInterfaceType.getTypeArguments().orElseThrow(() -> new InjectionProcessException(PROVIDER_TYPE_NOT_EXIST)).get(0);
            if (type.isClassOrInterfaceType() && processorManager.getQualifiedNameByType(type).equals(Mono.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getMonoProvider")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(type.asClassOrInterfaceType().getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0))));

            } else if (type.isClassOrInterfaceType() && processorManager.getQualifiedNameByType(type).equals(PublisherBuilder.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getPublisherBuilderProvider")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(type.asClassOrInterfaceType().getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0))));

            } else {
                methodCallExpr = new MethodCallExpr()
                        .setName("getProvider")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(type)));
            }
            belongCompilationUnit.addImport(Provider.class);
        } else {
            if (processorManager.getQualifiedNameByType(classOrInterfaceType).equals(Mono.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getMono")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(classOrInterfaceType.getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0))));
            } else if (processorManager.getQualifiedNameByType(classOrInterfaceType).equals(PublisherBuilder.class.getName())) {
                methodCallExpr = new MethodCallExpr()
                        .setName("getPublisherBuilder")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(classOrInterfaceType.getTypeArguments().orElseThrow(() -> new InjectionProcessException(INSTANCE_TYPE_NOT_EXIST)).get(0))));
            } else {
                methodCallExpr = new MethodCallExpr()
                        .setName("get")
                        .setScope(new NameExpr().setName("BeanContext"))
                        .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByType(classOrInterfaceType)));
            }
        }
        nameStringExpr.ifPresent(methodCallExpr::addArgument);
        return methodCallExpr;
    }

    private Stream<CompilationUnit> buildProducesContextStream(Set<? extends Element> singletonSet,
                                                               Set<? extends Element> dependentSet,
                                                               Set<? extends Element> applicationScopedSet,
                                                               Set<? extends Element> requestScopedSet,
                                                               Set<? extends Element> sessionScopedSet,
                                                               Set<? extends Element> transactionScopedSet,
                                                               Set<? extends Element> configPropertiesSet) {

        return Streams.concat(singletonSet.stream(), dependentSet.stream(), applicationScopedSet.stream(), requestScopedSet.stream(), sessionScopedSet.stream(), transactionScopedSet.stream(), configPropertiesSet.stream())
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
                            ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                                    .setPublic(true)
                                    .setName(classOrInterfaceDeclaration.getNameAsString() + "_Context")
                                    .addAnnotation(
                                            new SingleMemberAnnotationExpr()
                                                    .setMemberValue(new ClassExpr().setType(ModuleContext.class))
                                                    .setName(AutoService.class.getSimpleName())
                                    )
                                    .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()))
                                    .addExtendedType(BaseModuleContext.class);

                            CompilationUnit moduleContextCompilationUnit = new CompilationUnit()
                                    .addType(moduleContextInterfaceDeclaration)
                                    .addImport(AutoService.class)
                                    .addImport(ModuleContext.class)
                                    .addImport(BaseModuleContext.class)
                                    .addImport(Generated.class)
                                    .addImport("io.graphoenix.core.context.BeanContext");

                            compilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> moduleContextCompilationUnit.setPackageDeclaration(packageDeclaration.getNameAsString()));

                            BlockStmt staticInitializer = moduleContextInterfaceDeclaration.addStaticInitializer();

                            classOrInterfaceDeclaration.getMethods().stream()
                                    .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Produces.class))
                                    .forEach(producesMethodDeclaration -> {
                                                String qualifiedName = processorManager.getQualifiedNameByType(producesMethodDeclaration.getType());
                                                if (qualifiedName.equals(Mono.class.getName()) || qualifiedName.equals(PublisherBuilder.class.getName())) {
                                                    qualifiedName = producesMethodDeclaration.getType().resolve().asReferenceType().getTypeParametersMap().get(0).b.asReferenceType().getQualifiedName();
                                                }

                                                if (producesMethodDeclaration.isAnnotationPresent(Singleton.class) || producesMethodDeclaration.isAnnotationPresent(ApplicationScoped.class)) {
                                                    ClassOrInterfaceDeclaration holderClassOrInterfaceDeclaration = new ClassOrInterfaceDeclaration();
                                                    holderClassOrInterfaceDeclaration.setName(qualifiedName.replaceAll("\\.", "_") + "Holder");
                                                    holderClassOrInterfaceDeclaration.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
                                                    holderClassOrInterfaceDeclaration.addFieldWithInitializer(
                                                            qualifiedName,
                                                            "INSTANCE",
                                                            new MethodCallExpr()
                                                                    .setName(producesMethodDeclaration.getName())
                                                                    .setArguments(
                                                                            producesMethodDeclaration.getParameters().stream()
                                                                                    .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleContextCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                    .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                    .collect(Collectors.toCollection(NodeList::new))
                                                                    )
                                                                    .setScope(
                                                                            new MethodCallExpr()
                                                                                    .setName("get")
                                                                                    .setScope(new NameExpr().setName("BeanContext"))
                                                                                    .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByDeclaration(classOrInterfaceDeclaration)))
                                                                    )
                                                    )
                                                            .setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

                                                    moduleContextInterfaceDeclaration.addMember(holderClassOrInterfaceDeclaration);

                                                    addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, true);

                                                    processorManager.getMethodReturnReferenceType(producesMethodDeclaration)
                                                            .forEach(resolvedReferenceType ->
                                                                    addPutTypeProducerStatement(staticInitializer, resolvedReferenceType.getQualifiedName(), moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, true)
                                                            );

                                                    Optional<StringLiteralExpr> nameStringExpr = producesMethodDeclaration.getAnnotationByClass(Named.class)
                                                            .flatMap(processorManager::findAnnotationValue)
                                                            .map(Expression::asStringLiteralExpr);

                                                    if (nameStringExpr.isPresent()) {
                                                        addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, nameStringExpr.get(), true);
                                                    }

                                                    Optional<StringLiteralExpr> defaultStringExpr = producesMethodDeclaration.getAnnotationByClass(Default.class)
                                                            .map(annotationExpr -> new StringLiteralExpr("default"));

                                                    if (defaultStringExpr.isPresent()) {
                                                        addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, defaultStringExpr.get(), true);
                                                    }

                                                    processorManager.getCompilationUnitByQualifiedNameOptional(qualifiedName)
                                                            .flatMap(returnTypeCompilationUnit -> processorManager.getPublicClassOrInterfaceDeclarationOptional(returnTypeCompilationUnit))
                                                            .ifPresent(returnTypeClassOrInterfaceDeclaration -> {
                                                                        returnTypeClassOrInterfaceDeclaration.getExtendedTypes()
                                                                                .forEach(extendedType -> {
                                                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(extendedType);
                                                                                            addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, true);
                                                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, true));
                                                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, true));
                                                                                        }
                                                                                );

                                                                        returnTypeClassOrInterfaceDeclaration.getImplementedTypes()
                                                                                .forEach(implementedType -> {
                                                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(implementedType);
                                                                                            addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, true);
                                                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, true));
                                                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, true));
                                                                                        }
                                                                                );

                                                                    }
                                                            );
                                                } else {
                                                    addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, false);

                                                    Optional<StringLiteralExpr> nameStringExpr = producesMethodDeclaration.getAnnotationByClass(Named.class)
                                                            .flatMap(processorManager::findAnnotationValue)
                                                            .map(Expression::asStringLiteralExpr);
                                                    if (nameStringExpr.isPresent()) {
                                                        addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, nameStringExpr.get(), false);
                                                    }

                                                    Optional<StringLiteralExpr> defaultStringExpr = producesMethodDeclaration.getAnnotationByClass(Default.class)
                                                            .map(annotationExpr -> new StringLiteralExpr("default"));
                                                    if (defaultStringExpr.isPresent()) {
                                                        addPutTypeProducerStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, defaultStringExpr.get(), false);
                                                    }

                                                    processorManager.getCompilationUnitByQualifiedNameOptional(qualifiedName)
                                                            .flatMap(returnTypeCompilationUnit -> processorManager.getPublicClassOrInterfaceDeclarationOptional(returnTypeCompilationUnit))
                                                            .ifPresent(returnTypeClassOrInterfaceDeclaration -> {
                                                                        returnTypeClassOrInterfaceDeclaration.getExtendedTypes()
                                                                                .forEach(extendedType -> {
                                                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(extendedType);
                                                                                            addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, false);
                                                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, false));
                                                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, false));
                                                                                        }
                                                                                );

                                                                        returnTypeClassOrInterfaceDeclaration.getImplementedTypes()
                                                                                .forEach(implementedType -> {
                                                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(implementedType);
                                                                                            addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, null, false);
                                                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, false));
                                                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeProducerStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, producesMethodDeclaration, stringLiteralExpr, false));
                                                                                        }
                                                                                );

                                                                    }
                                                            );
                                                }
                                            }
                                    );
                            return moduleContextCompilationUnit;
                        }
                );
    }

    private CompilationUnit buildModuleContext(List<CompilationUnit> componentCompilationUnits) {

        ClassOrInterfaceDeclaration moduleContextInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(processorManager.getRootPackageName().replaceAll("\\.", "_") + "_Context")
                .addAnnotation(
                        new SingleMemberAnnotationExpr()
                                .setMemberValue(new ClassExpr().setType(ModuleContext.class))
                                .setName(AutoService.class.getSimpleName())
                )
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr(getClass().getName())).setName(Generated.class.getSimpleName()))
                .addExtendedType(BaseModuleContext.class);

        CompilationUnit moduleContextCompilationUnit = new CompilationUnit()
                .addType(moduleContextInterfaceDeclaration)
                .addImport(AutoService.class)
                .addImport(ModuleContext.class)
                .addImport(BaseModuleContext.class)
                .addImport(Generated.class)
                .addImport("io.graphoenix.core.context.BeanContext");

        BlockStmt staticInitializer = moduleContextInterfaceDeclaration.addStaticInitializer();

        componentCompilationUnits.forEach(compilationUnit ->
                processorManager.getPublicClassOrInterfaceDeclarationOptional(compilationUnit)
                        .ifPresent(classOrInterfaceDeclaration -> {
                                    String qualifiedName = processorManager.getQualifiedNameByDeclaration(classOrInterfaceDeclaration);
                                    if (classOrInterfaceDeclaration.isAnnotationPresent(Singleton.class) || classOrInterfaceDeclaration.isAnnotationPresent(ApplicationScoped.class)) {
                                        ClassOrInterfaceDeclaration holderClassOrInterfaceDeclaration = new ClassOrInterfaceDeclaration();
                                        holderClassOrInterfaceDeclaration.setName(qualifiedName.replaceAll("\\.", "_") + "Holder");
                                        holderClassOrInterfaceDeclaration.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
                                        holderClassOrInterfaceDeclaration.addFieldWithInitializer(
                                                qualifiedName,
                                                "INSTANCE",
                                                new ObjectCreationExpr()
                                                        .setType(qualifiedName + "Proxy")
                                                        .setArguments(
                                                                classOrInterfaceDeclaration.getConstructors().stream()
                                                                        .findFirst()
                                                                        .map(constructorDeclaration ->
                                                                                constructorDeclaration.getParameters().stream()
                                                                                        .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleContextCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                        .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                        .collect(Collectors.toCollection(NodeList::new))
                                                                        )
                                                                        .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(classOrInterfaceDeclaration.getNameAsString())))
                                                        )
                                        )
                                                .setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                                        moduleContextInterfaceDeclaration.addMember(holderClassOrInterfaceDeclaration);
                                        addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, true);

                                        Optional<StringLiteralExpr> nameStringExpr = classOrInterfaceDeclaration.getAnnotationByClass(Named.class)
                                                .flatMap(processorManager::findAnnotationValue)
                                                .map(Expression::asStringLiteralExpr);

                                        nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));

                                        Optional<StringLiteralExpr> defaultStringExpr = classOrInterfaceDeclaration.getAnnotationByClass(Default.class)
                                                .map(annotationExpr -> new StringLiteralExpr("default"));

                                        defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));

                                        classOrInterfaceDeclaration.getExtendedTypes()
                                                .forEach(extendedType -> {
                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(extendedType);
                                                            addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, true);
                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));
                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));
                                                        }
                                                );

                                        classOrInterfaceDeclaration.getImplementedTypes()
                                                .forEach(implementedType -> {
                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(implementedType);
                                                            addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, true);
                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));
                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, true));
                                                        }
                                                );
                                    } else if (!classOrInterfaceDeclaration.isAnnotationPresent(ConfigProperties.class)) {
                                        addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, false);

                                        Optional<StringLiteralExpr> nameStringExpr = classOrInterfaceDeclaration.getAnnotationByClass(Named.class)
                                                .flatMap(processorManager::findAnnotationValue)
                                                .map(Expression::asStringLiteralExpr);

                                        nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));

                                        Optional<StringLiteralExpr> defaultStringExpr = classOrInterfaceDeclaration.getAnnotationByClass(Default.class)
                                                .map(annotationExpr -> new StringLiteralExpr("default"));

                                        defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, qualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));

                                        classOrInterfaceDeclaration.getExtendedTypes()
                                                .forEach(extendedType -> {
                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(extendedType);
                                                            addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, false);
                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));
                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));
                                                        }
                                                );

                                        classOrInterfaceDeclaration.getImplementedTypes()
                                                .forEach(implementedType -> {
                                                            String putClassQualifiedName = processorManager.getQualifiedNameByType(implementedType);
                                                            addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, null, false);
                                                            nameStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));
                                                            defaultStringExpr.ifPresent(stringLiteralExpr -> addPutTypeStatement(staticInitializer, putClassQualifiedName, moduleContextCompilationUnit, classOrInterfaceDeclaration, stringLiteralExpr, false));
                                                        }
                                                );
                                    }
                                }
                        )
        );
        componentProxyProcessors.forEach(componentProxyProcessor -> {
                    Logger.debug("processModuleContext {}", componentProxyProcessor.getClass().getName());
                    componentProxyProcessor.processModuleContext(moduleContextCompilationUnit, moduleContextInterfaceDeclaration, staticInitializer);
                }
        );
        Logger.info("{} module context class build success", processorManager.getQualifiedNameByDeclaration(moduleContextInterfaceDeclaration));
        return moduleContextCompilationUnit;
    }

    private void addPutTypeStatement(BlockStmt staticInitializer, String putClassQualifiedName, CompilationUnit moduleContextCompilationUnit, ClassOrInterfaceDeclaration classOrInterfaceDeclaration, StringLiteralExpr nameStringExpr, boolean isSingleton) {
        String factoryName = null;
        if (classOrInterfaceDeclaration.isAnnotationPresent(RequestScoped.class)) {
            factoryName = "RequestScopeInstanceFactory";
        } else if (classOrInterfaceDeclaration.isAnnotationPresent(SessionScoped.class)) {
            factoryName = "SessionScopeInstanceFactory";
        } else if (classOrInterfaceDeclaration.isAnnotationPresent(TransactionScoped.class)) {
            factoryName = "TransactionScopeInstanceFactory";
        }
        String qualifiedName = processorManager.getQualifiedNameByDeclaration(classOrInterfaceDeclaration);
        Expression supplierExpression;
        if (isSingleton) {
            supplierExpression = new LambdaExpr()
                    .setEnclosingParameters(true)
                    .setBody(
                            new ExpressionStmt()
                                    .setExpression(
                                            new FieldAccessExpr()
                                                    .setName("INSTANCE")
                                                    .setScope(new NameExpr(qualifiedName.replaceAll("\\.", "_") + "Holder"))
                                    )
                    );
        } else {
            supplierExpression = new LambdaExpr()
                    .setEnclosingParameters(true)
                    .setBody(
                            new ExpressionStmt()
                                    .setExpression(
                                            factoryName != null ?
                                                    buildScopeFactoryGetMethodExpression(moduleContextCompilationUnit, classOrInterfaceDeclaration, factoryName) :
                                                    new ObjectCreationExpr()
                                                            .setType(qualifiedName + "Proxy")
                                                            .setArguments(
                                                                    classOrInterfaceDeclaration.getConstructors().stream()
                                                                            .findFirst()
                                                                            .map(constructorDeclaration ->
                                                                                    constructorDeclaration.getParameters().stream()
                                                                                            .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleContextCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                                            .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                                            .collect(Collectors.toCollection(NodeList::new))
                                                                            )
                                                                            .orElseThrow(() -> new InjectionProcessException(CONSTRUCTOR_NOT_EXIST.bind(classOrInterfaceDeclaration.getNameAsString())))
                                                            )
                                    )
                    );
        }
        if (nameStringExpr != null) {
            staticInitializer.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(putClassQualifiedName))
                            .addArgument(nameStringExpr)
                            .addArgument(supplierExpression)
            );
        } else {
            staticInitializer.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(putClassQualifiedName))
                            .addArgument(supplierExpression)
            );
        }
    }

    private void addPutTypeProducerStatement(BlockStmt staticInitializer, String putClassQualifiedName, CompilationUnit moduleContextCompilationUnit, ClassOrInterfaceDeclaration classOrInterfaceDeclaration, MethodDeclaration methodDeclaration, StringLiteralExpr nameStringExpr, boolean isSingleton) {
        Expression supplierExpression;
        String qualifiedName = processorManager.getQualifiedNameByType(methodDeclaration.getType());
        if (qualifiedName.equals(Mono.class.getName()) || qualifiedName.equals(PublisherBuilder.class.getName())) {
            qualifiedName = methodDeclaration.getType().resolve().asReferenceType().getTypeParametersMap().get(0).b.asReferenceType().getQualifiedName();
        }

        if (isSingleton) {
            supplierExpression = new LambdaExpr()
                    .setEnclosingParameters(true)
                    .setBody(
                            new ExpressionStmt()
                                    .setExpression(
                                            new FieldAccessExpr()
                                                    .setName("INSTANCE")
                                                    .setScope(new NameExpr(qualifiedName.replaceAll("\\.", "_") + "Holder"))
                                    )
                    );
        } else {
            supplierExpression = new LambdaExpr()
                    .setEnclosingParameters(true)
                    .setBody(
                            new ExpressionStmt()
                                    .setExpression(
                                            new MethodCallExpr()
                                                    .setName(methodDeclaration.getName())
                                                    .setArguments(
                                                            methodDeclaration.getParameters().stream()
                                                                    .map(parameter -> getBeanGetMethodCallExpr(parameter, moduleContextCompilationUnit, parameter.getType().asClassOrInterfaceType()))
                                                                    .map(methodCallExpr -> (Expression) methodCallExpr)
                                                                    .collect(Collectors.toCollection(NodeList::new))
                                                    )
                                                    .setScope(
                                                            new MethodCallExpr()
                                                                    .setName("get")
                                                                    .setScope(new NameExpr().setName("BeanContext"))
                                                                    .addArgument(new ClassExpr().setType(processorManager.getQualifiedNameByDeclaration(classOrInterfaceDeclaration)))
                                                    )
                                    )
                    );
        }
        if (nameStringExpr != null) {
            staticInitializer.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(putClassQualifiedName))
                            .addArgument(nameStringExpr)
                            .addArgument(supplierExpression)
            );
        } else {
            staticInitializer.addStatement(
                    new MethodCallExpr()
                            .setName("put")
                            .addArgument(new ClassExpr().setType(putClassQualifiedName))
                            .addArgument(supplierExpression)
            );
        }
    }
}
