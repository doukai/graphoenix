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
                                Type type = methodDeclaration.getType();
                                String typeName = type.asString();
                                if (getClassName(typeName).equals(PublisherBuilder.class.getSimpleName()) ||
                                        getClassName(typeName).equals(Mono.class.getSimpleName())) {
                                    typeName = getArgumentClassNames(typeName)[0];
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
                                Type type = methodDeclaration.getType();
                                String typeName = type.asString();
                                if (getClassName(typeName).equals(PublisherBuilder.class.getSimpleName()) ||
                                        getClassName(typeName).equals(Mono.class.getSimpleName())) {
                                    typeName = getArgumentClassNames(typeName)[0];
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
                                Type type = methodDeclaration.getType();
                                String typeName = type.asString();
                                if (getClassName(typeName).equals(PublisherBuilder.class.getSimpleName()) ||
                                        getClassName(typeName).equals(Mono.class.getSimpleName())) {
                                    typeName = getArgumentClassNames(typeName)[0];
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
        if (typeName.endsWith("[]")) {
            return "[".concat(getInvokeFieldTypeName(typeName.replace("[]", ""))).concat("]");
        } else if (getClassName(typeName).equals(Collection.class.getSimpleName()) ||
                getClassName(typeName).equals(List.class.getSimpleName()) ||
                getClassName(typeName).equals(Set.class.getSimpleName()) ||
                getClassName(typeName).equals(Flux.class.getSimpleName())) {
            return "[".concat(getInvokeFieldTypeName(getArgumentClassNames(typeName)[0])).concat("]");
        } else if (getClassName(typeName).equals(int.class.getSimpleName()) ||
                getClassName(typeName).equals(short.class.getSimpleName()) ||
                getClassName(typeName).equals(byte.class.getSimpleName()) ||
                getClassName(typeName).equals(Integer.class.getSimpleName()) ||
                getClassName(typeName).equals(Short.class.getSimpleName()) ||
                getClassName(typeName).equals(Byte.class.getSimpleName())) {
            return "Int";
        } else if (getClassName(typeName).equals(float.class.getSimpleName()) ||
                getClassName(typeName).equals(double.class.getSimpleName()) ||
                getClassName(typeName).equals(Float.class.getSimpleName()) ||
                getClassName(typeName).equals(Double.class.getSimpleName())) {
            return "Float";
        } else if (getClassName(typeName).equals(String.class.getSimpleName()) ||
                getClassName(typeName).equals(char.class.getSimpleName()) ||
                getClassName(typeName).equals(Character.class.getSimpleName())) {
            return "String";
        } else if (getClassName(typeName).equals(boolean.class.getSimpleName()) ||
                getClassName(typeName).equals(Boolean.class.getSimpleName())) {
            return "Boolean";
        } else if (getClassName(typeName).equals(BigInteger.class.getSimpleName())) {
            return "BigInteger";
        } else if (getClassName(typeName).equals(BigDecimal.class.getSimpleName())) {
            return "BigDecimal";
        } else if (getClassName(typeName).equals(LocalDate.class.getSimpleName())) {
            return "Date";
        } else if (getClassName(typeName).equals(LocalTime.class.getSimpleName())) {
            return "Time";
        } else if (getClassName(typeName).equals(LocalDateTime.class.getSimpleName())) {
            return "DateTime";
        } else {
            return getClassName(typeName);
        }
    }

    private String getInvokeFieldArgumentTypeName(String className) {
        String invokeFieldTypeName = getInvokeFieldTypeName(className);
        if (manager.isObject(invokeFieldTypeName)) {
            return invokeFieldTypeName.concat("Input");
        }
        return invokeFieldTypeName;
    }

    public String getClassName(String className) {
        if (className.contains("<")) {
            int index = className.indexOf('<');
            return className.substring(0, index);
        } else {
            return className;
        }
    }

    public String[] getArgumentClassNames(String className) {
        if (className.contains("<")) {
            int index = className.indexOf('<');
            String argumentClassNames = className.substring(index + 1, className.length() - 1);
            if (argumentClassNames.contains(",")) {
                List<String> argumentClassNameList = new ArrayList<>();
                Arrays.stream(argumentClassNames.split(","))
                        .forEach(argumentClassName -> {
                                    if (argumentClassNameList.size() > 0) {
                                        int lastIndex = argumentClassNameList.size() - 1;
                                        String lastArgumentClassName = argumentClassNameList.get(lastIndex);
                                        if (lastArgumentClassName.contains("<") && !lastArgumentClassName.contains(">")) {
                                            argumentClassNameList.set(lastIndex, lastArgumentClassName + "," + argumentClassName);
                                        } else {
                                            argumentClassNameList.add(argumentClassName);
                                        }
                                    } else {
                                        argumentClassNameList.add(argumentClassName);
                                    }
                                }
                        );
                return argumentClassNameList.toArray(new String[]{});
            } else {
                return new String[]{argumentClassNames};
            }
        } else {
            return null;
        }
    }
}
