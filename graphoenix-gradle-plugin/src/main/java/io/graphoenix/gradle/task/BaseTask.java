package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.document.Directive;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.EnumValue;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.document.InterfaceType;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.annotation.Package;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.eclipse.microprofile.graphql.*;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.CLASS_INFO_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CONTAINER_TYPE_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INVOKE_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.MUTATION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;

public class BaseTask extends DefaultTask {

    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;

    protected static final String MAIN_PATH = "src".concat(File.separator).concat("main");
    protected static final String MAIN_JAVA_PATH = MAIN_PATH.concat(File.separator).concat("java");
    protected static final String MAIN_RESOURCES_PATH = MAIN_PATH.concat(File.separator).concat("resources");

    protected void init() {
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcePath = sourceSet.getResources().getSourceDirectories().getAsPath();
        ClassLoader classLoader = createClassLoader();
        CONFIG_UTIL.load(resourcePath);
        BeanContext.load(classLoader);
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        manager = BeanContext.get(IGraphQLDocumentManager.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        documentBuilder = BeanContext.get(DocumentBuilder.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);

        try {
            if (graphQLConfig.getPackageName() == null) {
                findDefaultPackageName().ifPresent(graphQLConfig::setPackageName);
            }
            manager.clearAll();
            configRegister.registerConfig(resourcePath);
            mapper.registerFieldMaps();
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }

    protected ClassLoader createClassLoader() throws TaskExecutionException {
        List<URL> urls = new ArrayList<>();
        SourceSetContainer sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
        try {
            for (SourceSet sourceSet : sourceSets) {
                for (File file : sourceSet.getCompileClasspath()) {
                    urls.add(file.toURI().toURL());
                }
                for (File classesDir : sourceSet.getOutput().getClassesDirs()) {
                    urls.add(classesDir.toURI().toURL());
                }
            }
        } catch (MalformedURLException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    protected List<CompilationUnit> buildCompilationUnits() throws IOException {
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains(MAIN_JAVA_PATH)).getAsPath();
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(Path.of(javaPath));
        ClassLoaderTypeSolver classLoaderTypeSolver = new ClassLoaderTypeSolver(createClassLoader());
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(javaParserTypeSolver);
        combinedTypeSolver.add(classLoaderTypeSolver);
        combinedTypeSolver.add(reflectionTypeSolver);
        JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        SourceRoot sourceRoot = new SourceRoot(Path.of(javaPath));
        sourceRoot.getParserConfiguration().setSymbolResolver(javaSymbolSolver);
        sourceRoot.tryToParse();
        return sourceRoot.getCompilationUnits();
    }

    public Optional<String> findDefaultPackageName() throws IOException {
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains(MAIN_JAVA_PATH)).getAsPath();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(Path.of(javaPath));
        SourceRoot sourceRoot = new SourceRoot(Path.of(javaPath));
        JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(javaParserTypeSolver);
        sourceRoot.getParserConfiguration().setSymbolResolver(javaSymbolSolver);
        sourceRoot.tryToParse();

        return sourceRoot.getCompilationUnits().stream()
                .flatMap(compilationUnit -> compilationUnit.getPackageDeclaration().stream())
                .filter(packageDeclaration -> packageDeclaration.getAnnotationByClass(Package.class).isPresent())
                .findFirst()
                .map(NodeWithName::getNameAsString);
    }

    protected void registerInvoke() throws IOException {
        registerInvoke(buildCompilationUnits());
    }

    protected void registerInvoke(List<CompilationUnit> compilations) {
        List<ResolvedReferenceTypeDeclaration> objectTypeList = compilations.stream()
                .flatMap(compilationUnit -> compilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class)))
                .flatMap(typeDeclaration -> typeDeclaration.getMethods().stream())
                .flatMap(methodDeclaration -> resolve(methodDeclaration.getType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Type.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> interfaceTypeList = compilations.stream()
                .flatMap(compilationUnit -> compilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class)))
                .flatMap(typeDeclaration -> typeDeclaration.getMethods().stream())
                .flatMap(methodDeclaration -> resolve(methodDeclaration.getType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> enumTypeList = compilations.stream()
                .flatMap(compilationUnit -> compilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class)))
                .flatMap(typeDeclaration -> typeDeclaration.getMethods().stream())
                .flatMap(methodDeclaration -> resolve(methodDeclaration.getType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Enum.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        objectTypeList.stream().map(this::buildObject).map(ObjectType::toString).forEach(manager::mergeDocument);
        interfaceTypeList.stream().map(this::buildInterface).map(InterfaceType::toString).forEach(manager::mergeDocument);
        enumTypeList.stream().map(this::buildEnum).map(EnumType::toString).forEach(manager::mergeDocument);

        objectTypeList.forEach(this::registerFields);
        interfaceTypeList.forEach(this::registerFields);

        compilations.stream()
                .flatMap(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                )
                .flatMap(typeDeclaration ->
                        typeDeclaration.getMethods().stream()
                                .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Query.class))
                                .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Mutation.class))
                                .filter(methodDeclaration -> methodDeclaration.getParameters().stream().anyMatch(parameter -> parameter.isAnnotationPresent(Source.class)))
                )
                .forEach(methodDeclaration -> {
                            String objectName = methodDeclaration.getParameters().stream()
                                    .filter(parameter -> parameter.isAnnotationPresent(Source.class))
                                    .findFirst()
                                    .orElseThrow(() -> new RuntimeException("@Source annotation parameter not exist in " + methodDeclaration.getNameAsString()))
                                    .getType()
                                    .asString();

                            GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(objectName)));
                            ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext)
                                    .addField(
                                            new Field()
                                                    .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                    .setTypeName(getInvokeFieldTypeName(methodDeclaration.getType().asString()))
                                                    .addDirective(new Directive().setName(INVOKE_DIRECTIVE_NAME))
                                    );
                            manager.mergeDocument(objectType.toString());
                        }
                );

        compilations.stream()
                .flatMap(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                )
                .flatMap(typeDeclaration ->
                        typeDeclaration.getMethods().stream()
                                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Query.class))
                )
                .forEach(methodDeclaration -> {
                            ObjectType objectType = manager.getQueryOperationTypeName()
                                    .flatMap(manager::getObject)
                                    .map(documentBuilder::buildObject)
                                    .orElseGet(() -> new ObjectType().setName(QUERY_TYPE_NAME))
                                    .addField(
                                            new Field()
                                                    .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                    .setTypeName(getInvokeFieldTypeName(methodDeclaration.getType().asString()))
                                                    .setArguments(
                                                            methodDeclaration.getParameters().stream()
                                                                    .map(parameter ->
                                                                            new InputValue()
                                                                                    .setName(parameter.getName().getIdentifier())
                                                                                    .setTypeName(getInvokeFieldArgumentTypeName(parameter.getType().asString())))
                                                                    .collect(Collectors.toCollection(LinkedHashSet::new))
                                                    )
                                                    .addDirective(new Directive().setName(INVOKE_DIRECTIVE_NAME))
                                    );
                            manager.mergeDocument(objectType.toString());
                        }
                );

        compilations.stream()
                .flatMap(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                )
                .flatMap(typeDeclaration ->
                        typeDeclaration.getMethods().stream()
                                .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Mutation.class))
                )
                .forEach(methodDeclaration -> {
                            ObjectType objectType = manager.getMutationOperationTypeName()
                                    .flatMap(manager::getObject)
                                    .map(documentBuilder::buildObject)
                                    .orElseGet(() -> new ObjectType().setName(MUTATION_TYPE_NAME))
                                    .addField(
                                            new Field()
                                                    .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                    .setTypeName(getInvokeFieldTypeName(methodDeclaration.getType().asString()))
                                                    .setArguments(
                                                            methodDeclaration.getParameters().stream()
                                                                    .map(parameter ->
                                                                            new InputValue()
                                                                                    .setName(parameter.getName().getIdentifier())
                                                                                    .setTypeName(getInvokeFieldArgumentTypeName(parameter.getType().asString())))
                                                                    .collect(Collectors.toCollection(LinkedHashSet::new))
                                                    )
                                                    .addDirective(new Directive().setName(INVOKE_DIRECTIVE_NAME))
                                    );
                            manager.mergeDocument(objectType.toString());
                        }
                );
    }

    protected void registerFields(ResolvedReferenceTypeDeclaration parentResolvedReferenceTypeDeclaration) {
        List<ResolvedReferenceTypeDeclaration> objectTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Type.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> interfaceTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> enumTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Enum.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .collect(Collectors.toList());

        objectTypeList.stream().map(this::buildObject).map(ObjectType::toString).forEach(manager::mergeDocument);
        interfaceTypeList.stream().map(this::buildInterface).map(InterfaceType::toString).forEach(manager::mergeDocument);
        enumTypeList.stream().map(this::buildEnum).map(EnumType::toString).forEach(manager::mergeDocument);

        objectTypeList.forEach(this::registerFields);
        interfaceTypeList.forEach(this::registerFields);
    }

    protected Optional<ResolvedReferenceTypeDeclaration> resolve(Type type) {
        if (type.isArrayType()) {
            return resolve(type.asArrayType().getElementType());
        } else if (type.isReferenceType()) {
            try {
                ResolvedReferenceType resolvedReferenceType = type.resolve().asReferenceType();
                return resolve(resolvedReferenceType);
            } catch (UnsolvedSymbolException e) {
                Logger.warn(e);
            }
        }
        return Optional.empty();
    }

    protected Optional<ResolvedReferenceTypeDeclaration> resolve(ResolvedReferenceType resolvedReferenceType) {
        String qualifiedName = resolvedReferenceType.getQualifiedName();
        if (qualifiedName.equals(PublisherBuilder.class.getCanonicalName()) ||
                qualifiedName.equals(Mono.class.getCanonicalName()) ||
                qualifiedName.equals(Flux.class.getCanonicalName()) ||
                qualifiedName.equals(Collection.class.getCanonicalName()) ||
                qualifiedName.equals(List.class.getCanonicalName()) ||
                qualifiedName.equals(Set.class.getCanonicalName())) {
            return resolve(resolvedReferenceType.typeParametersValues().get(0).asReferenceType());
        } else {
            return resolvedReferenceType.getTypeDeclaration();
        }
    }

    protected ObjectType buildObject(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        return new ObjectType()
                .setName(resolvedReferenceTypeDeclaration.getName())
                .setFields(
                        resolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                                .map(this::buildField)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                )
                .addDirective(
                        new Directive()
                                .setName(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected InterfaceType buildInterface(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        return new InterfaceType()
                .setName(resolvedReferenceTypeDeclaration.getName())
                .setFields(
                        resolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                                .map(this::buildField)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                )
                .addDirective(
                        new Directive()
                                .setName(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected EnumType buildEnum(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        return new EnumType()
                .setName(resolvedReferenceTypeDeclaration.getName())
                .setEnumValues(
                        resolvedReferenceTypeDeclaration.asEnum().getEnumConstants().stream()
                                .map(this::buildEnumValue)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                )
                .addDirective(
                        new Directive()
                                .setName(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected Field buildField(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        return new Field(getInvokeFieldName(resolvedMethodDeclaration.getName()))
                .setTypeName(getInvokeFieldTypeName(resolvedMethodDeclaration.getReturnType().toString()));
    }

    protected EnumValue buildEnumValue(ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration) {
        return new EnumValue(getInvokeFieldName(resolvedEnumConstantDeclaration.getName()));
    }

    private String getInvokeFieldName(String methodName) {
        if (methodName.startsWith("get")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("get", ""));
        } else if (methodName.startsWith("set")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("set", ""));
        } else {
            return methodName;
        }
    }

    private String getInvokeFieldTypeName(String typeName) {
        String className = TYPE_NAME_UTIL.getClassName(typeName);
        if (typeName.endsWith("[]")) {
            return "[".concat(getInvokeFieldTypeName(typeName.replace("[]", ""))).concat("]");
        } else if (className.equals(PublisherBuilder.class.getSimpleName()) || className.equals(Mono.class.getSimpleName())) {
            return getInvokeFieldTypeName(TYPE_NAME_UTIL.getArgumentTypeName0(typeName));
        } else if (className.equals(Collection.class.getSimpleName()) ||
                className.equals(List.class.getSimpleName()) ||
                className.equals(Set.class.getSimpleName()) ||
                className.equals(Flux.class.getSimpleName())) {
            return "[".concat(getInvokeFieldTypeName(TYPE_NAME_UTIL.getArgumentTypeName0(typeName))).concat("]");
        } else if (className.equals(int.class.getSimpleName()) ||
                className.equals(short.class.getSimpleName()) ||
                className.equals(byte.class.getSimpleName()) ||
                className.equals(Integer.class.getSimpleName()) ||
                className.equals(Short.class.getSimpleName()) ||
                className.equals(Byte.class.getSimpleName())) {
            return "Int";
        } else if (className.equals(float.class.getSimpleName()) ||
                className.equals(double.class.getSimpleName()) ||
                className.equals(Float.class.getSimpleName()) ||
                className.equals(Double.class.getSimpleName())) {
            return "Float";
        } else if (className.equals(String.class.getSimpleName()) ||
                className.equals(char.class.getSimpleName()) ||
                className.equals(Character.class.getSimpleName())) {
            return "String";
        } else if (className.equals(boolean.class.getSimpleName()) ||
                className.equals(Boolean.class.getSimpleName())) {
            return "Boolean";
        } else if (className.equals(BigInteger.class.getSimpleName())) {
            return "BigInteger";
        } else if (className.equals(BigDecimal.class.getSimpleName())) {
            return "BigDecimal";
        } else if (className.equals(LocalDate.class.getSimpleName())) {
            return "Date";
        } else if (className.equals(LocalTime.class.getSimpleName())) {
            return "Time";
        } else if (className.equals(LocalDateTime.class.getSimpleName())) {
            return "DateTime";
        } else {
            return className;
        }
    }

    private String getInvokeFieldArgumentTypeName(String className) {
        String invokeFieldTypeName = getInvokeFieldTypeName(className);
        if (manager.isObject(invokeFieldTypeName)) {
            return invokeFieldTypeName.concat("Input");
        }
        return invokeFieldTypeName;
    }
}
