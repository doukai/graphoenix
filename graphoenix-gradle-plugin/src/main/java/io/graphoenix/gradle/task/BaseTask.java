package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.Type;
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
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
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
import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

public class BaseTask extends DefaultTask {

    private final IGraphQLDocumentManager manager;
    private final GraphQLConfigRegister configRegister;
    private final DocumentBuilder documentBuilder;
    private final IGraphQLFieldMapManager mapper;

    public BaseTask() {
        manager = BeanContext.get(IGraphQLDocumentManager.class);
        configRegister = BeanContext.get(GraphQLConfigRegister.class);
        documentBuilder = BeanContext.get(DocumentBuilder.class);
        mapper = BeanContext.get(IGraphQLFieldMapManager.class);
    }

    protected void init() throws IOException, URISyntaxException {
        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        assert graphQLConfig != null;
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcesPath = sourceSet.getResources().getSourceDirectories().getAsPath();

        configRegister.registerPreset(createClassLoader());
        configRegister.registerConfig(graphQLConfig, resourcesPath);
        if (graphQLConfig.getBuild()) {
            manager.registerGraphQL(documentBuilder.buildDocument().toString());
        }
        mapper.registerFieldMaps();
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

    protected void registerInvoke(IGraphQLDocumentManager manager) {
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\java")).getAsPath();
        try {
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
            List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
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
                                        .orElseThrow()
                                        .getType()
                                        .asString();

                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(objectName)));
                                String typeName = methodDeclaration.getType().asString();
                                String className = TYPE_NAME_UTIL.getClassName(typeName);
                                if (className.equals(PublisherBuilder.class.getSimpleName()) ||
                                        className.equals(Mono.class.getSimpleName())) {
                                    typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                                }
                                String invokeFieldTypeName = getInvokeFieldTypeName(typeName);
                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext)
                                        .addField(
                                                new Field()
                                                        .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                        .setTypeName(invokeFieldTypeName)
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
                                String typeName = methodDeclaration.getType().asString();
                                String className = TYPE_NAME_UTIL.getClassName(typeName);
                                if (className.equals(PublisherBuilder.class.getSimpleName()) ||
                                        className.equals(Mono.class.getSimpleName())) {
                                    typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                                }
                                String invokeFieldTypeName = getInvokeFieldTypeName(typeName);
                                ObjectType objectType = manager.getQueryOperationTypeName().flatMap(manager::getObject)
                                        .map(documentBuilder::buildObject)
                                        .orElseGet(() -> new ObjectType().setName("QueryType"));
                                objectType.addField(
                                        new Field()
                                                .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                .setTypeName(invokeFieldTypeName)
                                                .setArguments(
                                                        methodDeclaration.getParameters().stream()
                                                                .map(parameter ->
                                                                        new InputValue()
                                                                                .setName(parameter.getName().getIdentifier())
                                                                                .setTypeName(getInvokeFieldArgumentTypeName(parameter.getType().asString())))
                                                                .collect(Collectors.toCollection(LinkedHashSet::new))
                                                )
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
                                String typeName = methodDeclaration.getType().asString();
                                String className = TYPE_NAME_UTIL.getClassName(typeName);
                                if (className.equals(PublisherBuilder.class.getSimpleName()) ||
                                        className.equals(Mono.class.getSimpleName())) {
                                    typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                                }
                                String invokeFieldTypeName = getInvokeFieldTypeName(typeName);
                                ObjectType objectType = manager.getMutationOperationTypeName().flatMap(manager::getObject)
                                        .map(documentBuilder::buildObject)
                                        .orElseGet(() -> new ObjectType().setName("MutationType"));
                                objectType.addField(
                                        new Field()
                                                .setName(getInvokeFieldName(methodDeclaration.getName().getIdentifier()))
                                                .setTypeName(invokeFieldTypeName)
                                                .setArguments(
                                                        methodDeclaration.getParameters().stream()
                                                                .map(parameter ->
                                                                        new InputValue()
                                                                                .setName(parameter.getName().getIdentifier())
                                                                                .setTypeName(getInvokeFieldArgumentTypeName(parameter.getType().asString())))
                                                                .collect(Collectors.toCollection(LinkedHashSet::new))
                                                )
                                );
                                manager.mergeDocument(objectType.toString());
                            }
                    );
        } catch (IOException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
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
