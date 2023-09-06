package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.EnumValue;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.document.InterfaceType;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.annotation.Package;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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

import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.BYTE;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.CHAR;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.DOUBLE;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.FLOAT;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.INT;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.LONG;
import static com.github.javaparser.resolution.types.ResolvedPrimitiveType.SHORT;
import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;

public class BaseTask extends DefaultTask {

    private GraphQLConfig graphQLConfig;
    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;

    protected static final String MAIN_PATH = "src".concat(File.separator).concat("main");
    protected static final String MAIN_JAVA_PATH = MAIN_PATH.concat(File.separator).concat("java");
    protected static final String MAIN_RESOURCES_PATH = MAIN_PATH.concat(File.separator).concat("resources");

    protected void init() {
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcePath = sourceSet.getResources().getSourceDirectories().getAsPath();

        try {
            CONFIG_UTIL.load(resourcePath);
            ClassLoader classLoader = createClassLoader();
            BeanContext.load(classLoader);
            graphQLConfig = BeanContext.get(GraphQLConfig.class);
            findDefaultPackageName().ifPresent(graphQLConfig::setPackageName);
            manager = BeanContext.get(IGraphQLDocumentManager.class);
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            documentBuilder = BeanContext.get(DocumentBuilder.class);

            manager.clearAll();
            configRegister.registerConfig(resourcePath);
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
                .filter(resolvedReferenceTypeDeclaration -> manager.getObject(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> interfaceTypeList = compilations.stream()
                .flatMap(compilationUnit -> compilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class)))
                .flatMap(typeDeclaration -> typeDeclaration.getMethods().stream())
                .flatMap(methodDeclaration -> resolve(methodDeclaration.getType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> manager.getInterface(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> enumTypeList = compilations.stream()
                .flatMap(compilationUnit -> compilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class)))
                .flatMap(typeDeclaration -> typeDeclaration.getMethods().stream())
                .flatMap(methodDeclaration -> resolve(methodDeclaration.getType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Enum.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> manager.getEnum(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
                .collect(Collectors.toList());

        objectTypeList.stream().map(this::buildObject).map(ObjectType::toString).forEach(manager::mergeDocument);
        interfaceTypeList.stream().map(this::buildInterface).map(InterfaceType::toString).forEach(manager::mergeDocument);
        enumTypeList.stream().map(this::buildEnum).map(EnumType::toString).forEach(manager::mergeDocument);

        objectTypeList.forEach(this::registerFields);
        interfaceTypeList.forEach(this::registerFields);

        compilations.forEach(compilationUnit ->
                compilationUnit.getTypes().stream()
                        .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                        .forEach(typeDeclaration ->
                                typeDeclaration.getMethods().stream()
                                        .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Query.class))
                                        .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Mutation.class))
                                        .filter(methodDeclaration ->
                                                methodDeclaration.getParameters().stream()
                                                        .anyMatch(parameter ->
                                                                parameter.isAnnotationPresent(Source.class) &&
                                                                        parameter.getType().isClassOrInterfaceType() &&
                                                                        parameter.getType().resolve().asReferenceType().getTypeDeclaration().stream()
                                                                                .anyMatch(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Type.class.getCanonicalName()))
                                                        )
                                        )
                                        .forEach(methodDeclaration -> {
                                                    String objectName = methodDeclaration.getParameters().stream()
                                                            .filter(parameter -> parameter.isAnnotationPresent(Source.class))
                                                            .filter(parameter -> parameter.getType().isClassOrInterfaceType())
                                                            .filter(parameter ->
                                                                    parameter.getType().resolve().asReferenceType().getTypeDeclaration().stream()
                                                                            .anyMatch(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Type.class.getCanonicalName()))
                                                            )
                                                            .findFirst()
                                                            .orElseThrow(() -> new RuntimeException("@Source annotation parameter not exist in " + methodDeclaration.getNameAsString()))
                                                            .getType()
                                                            .asString();

                                                    GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(objectName)));
                                                    ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext)
                                                            .addField(
                                                                    new Field()
                                                                            .setName(getSourceNameFromMethodDeclaration(methodDeclaration).orElseGet(() -> getInvokeFieldName(methodDeclaration.getName().getIdentifier())))
                                                                            .setType(getInvokeFieldTypeName(methodDeclaration.getType()))
                                                                            .addDirective(
                                                                                    new Directive().setName(INVOKE_DIRECTIVE_NAME)
                                                                                            .addArgument("className",
                                                                                                    typeDeclaration.getFullyQualifiedName()
                                                                                                            .orElseGet(() ->
                                                                                                                    compilationUnit.getPackageDeclaration()
                                                                                                                            .map(packageDeclaration -> packageDeclaration.getNameAsString().concat("."))
                                                                                                                            .orElse("")
                                                                                                                            .concat(typeDeclaration.getNameAsString())
                                                                                                            )
                                                                                            )
                                                                                            .addArgument("methodName", methodDeclaration.getNameAsString())
                                                                                            .addArgument(
                                                                                                    "parameters",
                                                                                                    new ArrayValueWithVariable(
                                                                                                            methodDeclaration.getParameters().stream()
                                                                                                                    .map(parameter -> Map.of("name", parameter.getNameAsString(), "className", getTypeName(parameter.getType())))
                                                                                                                    .collect(Collectors.toList())
                                                                                                    )
                                                                                            )
                                                                                            .addArgument("returnClassName", getTypeName(methodDeclaration.getType()))
                                                                            )
                                                                            .addDirective(
                                                                                    new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                                                                            .addArgument("packageName", graphQLConfig.getPackageName())
                                                                                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                                                                            )
                                                            );
                                                    manager.mergeDocument(objectType.toString());
                                                }
                                        )
                        )
        );

        compilations.forEach(compilationUnit ->
                compilationUnit.getTypes().stream()
                        .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                        .forEach(typeDeclaration ->
                                typeDeclaration.getMethods().stream()
                                        .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Query.class))
                                        .filter(methodDeclaration -> !methodDeclaration.isAnnotationPresent(Mutation.class))
                                        .filter(methodDeclaration ->
                                                methodDeclaration.getParameters().stream()
                                                        .anyMatch(parameter ->
                                                                parameter.isAnnotationPresent(Source.class) &&
                                                                        parameter.getType().isClassOrInterfaceType() &&
                                                                        parameter.getType().resolve().asReferenceType().getTypeDeclaration().stream()
                                                                                .anyMatch(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                                                        )
                                        )
                                        .forEach(methodDeclaration -> {
                                                    String interfaceName = methodDeclaration.getParameters().stream()
                                                            .filter(parameter -> parameter.isAnnotationPresent(Source.class))
                                                            .filter(parameter -> parameter.getType().isClassOrInterfaceType())
                                                            .filter(parameter ->
                                                                    parameter.getType().resolve().asReferenceType().getTypeDeclaration().stream()
                                                                            .anyMatch(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                                                            )
                                                            .findFirst()
                                                            .orElseThrow(() -> new RuntimeException("@Source annotation parameter not exist in " + methodDeclaration.getNameAsString()))
                                                            .getType()
                                                            .asString();

                                                    manager.getImplementsObjectType(interfaceName)
                                                            .forEach(objectTypeDefinitionContext -> {
                                                                        ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext)
                                                                                .addField(
                                                                                        new Field()
                                                                                                .setName(getSourceNameFromMethodDeclaration(methodDeclaration).orElseGet(() -> getInvokeFieldName(methodDeclaration.getName().getIdentifier())))
                                                                                                .setType(getInvokeFieldTypeName(methodDeclaration.getType()))
                                                                                                .addDirective(
                                                                                                        new Directive().setName(INVOKE_DIRECTIVE_NAME)
                                                                                                                .addArgument("className",
                                                                                                                        typeDeclaration.getFullyQualifiedName()
                                                                                                                                .orElseGet(() ->
                                                                                                                                        compilationUnit.getPackageDeclaration()
                                                                                                                                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat("."))
                                                                                                                                                .orElse("")
                                                                                                                                                .concat(typeDeclaration.getNameAsString())
                                                                                                                                )
                                                                                                                )
                                                                                                                .addArgument("methodName", methodDeclaration.getNameAsString())
                                                                                                                .addArgument(
                                                                                                                        "parameters",
                                                                                                                        new ArrayValueWithVariable(
                                                                                                                                methodDeclaration.getParameters().stream()
                                                                                                                                        .map(parameter -> Map.of("name", parameter.getNameAsString(), "className", getTypeName(parameter.getType())))
                                                                                                                                        .collect(Collectors.toList())
                                                                                                                        )
                                                                                                                )
                                                                                                                .addArgument("returnClassName", getTypeName(methodDeclaration.getType()))
                                                                                                )
                                                                                                .addDirective(
                                                                                                        new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                                                                                                .addArgument("packageName", graphQLConfig.getPackageName())
                                                                                                                .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                                                                                                )
                                                                                );
                                                                        manager.mergeDocument(objectType.toString());
                                                                    }
                                                            );
                                                }
                                        )
                        )
        );

        compilations.forEach(compilationUnit ->
                compilationUnit.getTypes().stream()
                        .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                        .forEach(typeDeclaration ->
                                typeDeclaration.getMethods().stream()
                                        .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Query.class))
                                        .forEach(methodDeclaration -> {
                                                    ObjectType objectType = manager.getObject(manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME))
                                                            .map(documentBuilder::buildObject)
                                                            .orElseGet(() -> new ObjectType(QUERY_TYPE_NAME))
                                                            .addField(
                                                                    new Field()
                                                                            .setName(getQueryNameFromMethodDeclaration(methodDeclaration).orElseGet(() -> getInvokeFieldName(methodDeclaration.getName().getIdentifier())))
                                                                            .setType(getInvokeFieldTypeName(methodDeclaration.getType()))
                                                                            .setArguments(
                                                                                    methodDeclaration.getParameters().stream()
                                                                                            .map(parameter ->
                                                                                                    new InputValue()
                                                                                                            .setName(parameter.getName().getIdentifier())
                                                                                                            .setType(getInvokeFieldArgumentTypeName(parameter.getType())))
                                                                                            .collect(Collectors.toCollection(LinkedHashSet::new))
                                                                            )
                                                                            .addDirective(
                                                                                    new Directive().setName(INVOKE_DIRECTIVE_NAME)
                                                                                            .addArgument("className",
                                                                                                    typeDeclaration.getFullyQualifiedName()
                                                                                                            .orElseGet(() ->
                                                                                                                    compilationUnit.getPackageDeclaration()
                                                                                                                            .map(packageDeclaration -> packageDeclaration.getNameAsString().concat("."))
                                                                                                                            .orElse("")
                                                                                                                            .concat(typeDeclaration.getNameAsString())
                                                                                                            )
                                                                                            )
                                                                                            .addArgument("methodName", methodDeclaration.getNameAsString())
                                                                                            .addArgument(
                                                                                                    "parameters",
                                                                                                    new ArrayValueWithVariable(
                                                                                                            methodDeclaration.getParameters().stream()
                                                                                                                    .map(parameter -> Map.of("name", parameter.getNameAsString(), "className", getTypeName(parameter.getType())))
                                                                                                                    .collect(Collectors.toList())
                                                                                                    )
                                                                                            )
                                                                                            .addArgument("returnClassName", getTypeName(methodDeclaration.getType()))
                                                                            )
                                                                            .addDirective(
                                                                                    new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                                                                            .addArgument("packageName", graphQLConfig.getPackageName())
                                                                                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                                                                            )
                                                            );
                                                    manager.mergeDocument(objectType.toString());
                                                }
                                        )
                        )
        );

        compilations.forEach(compilationUnit ->
                compilationUnit.getTypes().stream()
                        .filter(typeDeclaration -> typeDeclaration.isAnnotationPresent(GraphQLApi.class))
                        .forEach(typeDeclaration ->
                                typeDeclaration.getMethods().stream()
                                        .filter(methodDeclaration -> methodDeclaration.isAnnotationPresent(Mutation.class))
                                        .forEach(methodDeclaration -> {
                                                    ObjectType objectType = manager.getObject(manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME))
                                                            .map(documentBuilder::buildObject)
                                                            .orElseGet(() -> new ObjectType(MUTATION_TYPE_NAME))
                                                            .addField(
                                                                    new Field()
                                                                            .setName(getMutationNameFromMethodDeclaration(methodDeclaration).orElseGet(() -> getInvokeFieldName(methodDeclaration.getName().getIdentifier())))
                                                                            .setType(getInvokeFieldTypeName(methodDeclaration.getType()))
                                                                            .setArguments(
                                                                                    methodDeclaration.getParameters().stream()
                                                                                            .map(parameter ->
                                                                                                    new InputValue()
                                                                                                            .setName(parameter.getName().getIdentifier())
                                                                                                            .setType(getInvokeFieldArgumentTypeName(parameter.getType())))
                                                                                            .collect(Collectors.toCollection(LinkedHashSet::new))
                                                                            )
                                                                            .addDirective(
                                                                                    new Directive().setName(INVOKE_DIRECTIVE_NAME)
                                                                                            .addArgument("className",
                                                                                                    typeDeclaration.getFullyQualifiedName()
                                                                                                            .orElseGet(() ->
                                                                                                                    compilationUnit.getPackageDeclaration()
                                                                                                                            .map(packageDeclaration -> packageDeclaration.getNameAsString().concat("."))
                                                                                                                            .orElse("")
                                                                                                                            .concat(typeDeclaration.getNameAsString())
                                                                                                            )
                                                                                            )
                                                                                            .addArgument("methodName", methodDeclaration.getNameAsString())
                                                                                            .addArgument(
                                                                                                    "parameters",
                                                                                                    new ArrayValueWithVariable(
                                                                                                            methodDeclaration.getParameters().stream()
                                                                                                                    .map(parameter -> Map.of("name", parameter.getNameAsString(), "className", getTypeName(parameter.getType())))
                                                                                                                    .collect(Collectors.toList())
                                                                                                    )
                                                                                            )
                                                                                            .addArgument("returnClassName", getTypeName(methodDeclaration.getType()))
                                                                            )
                                                                            .addDirective(
                                                                                    new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                                                                            .addArgument("packageName", graphQLConfig.getPackageName())
                                                                                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                                                                            )
                                                            );
                                                    manager.mergeDocument(objectType.toString());
                                                }
                                        )
                        )
        );
    }

    protected void registerFields(ResolvedReferenceTypeDeclaration parentResolvedReferenceTypeDeclaration) {
        List<ResolvedReferenceTypeDeclaration> objectTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Type.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> manager.getObject(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> interfaceTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Interface.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> manager.getInterface(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
                .collect(Collectors.toList());

        List<ResolvedReferenceTypeDeclaration> enumTypeList = parentResolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getReturnType().isReferenceType())
                .flatMap(resolvedMethodDeclaration -> resolve(resolvedMethodDeclaration.getReturnType().asReferenceType()).stream())
                .filter(resolvedReferenceTypeDeclaration -> resolvedReferenceTypeDeclaration.hasAnnotation(org.eclipse.microprofile.graphql.Enum.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> !resolvedReferenceTypeDeclaration.hasAnnotation(Ignore.class.getCanonicalName()))
                .filter(resolvedReferenceTypeDeclaration -> manager.getEnum(findTypeName(resolvedReferenceTypeDeclaration)).isEmpty())
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

    protected String getTypeName(Type type) {
        if (type.isArrayType()) {
            return getTypeName(type.asArrayType().getElementType()) + "[]";
        } else if (type.isPrimitiveType()) {
            return type.asPrimitiveType().resolve().name();
        } else if (type.isClassOrInterfaceType()) {
            return type.asClassOrInterfaceType().resolve().asReferenceType().getQualifiedName();
        }
        return type.asString();
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
                .setName(findTypeName(resolvedReferenceTypeDeclaration))
                .setFields(
                        resolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                                .filter(resolvedMethodDeclaration -> !resolvedMethodDeclaration.getReturnType().isVoid())
                                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getName().startsWith("get"))
                                .map(this::buildField)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                                .addArgument("exists", true)
                                .addArgument("grpcClassName", graphQLConfig.getGrpcObjectTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(resolvedReferenceTypeDeclaration.getClassName())))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected InterfaceType buildInterface(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        return new InterfaceType()
                .setName(findTypeName(resolvedReferenceTypeDeclaration))
                .setFields(
                        resolvedReferenceTypeDeclaration.getDeclaredMethods().stream()
                                .filter(resolvedMethodDeclaration -> !resolvedMethodDeclaration.getReturnType().isVoid())
                                .filter(resolvedMethodDeclaration -> resolvedMethodDeclaration.getName().startsWith("get"))
                                .map(this::buildField)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                                .addArgument("exists", true)
                                .addArgument("grpcClassName", graphQLConfig.getGrpcInterfaceTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(resolvedReferenceTypeDeclaration.getClassName())))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected EnumType buildEnum(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        return new EnumType()
                .setName(findTypeName(resolvedReferenceTypeDeclaration))
                .setEnumValues(
                        resolvedReferenceTypeDeclaration.asEnum().getEnumConstants().stream()
                                .map(this::buildEnumValue)
                                .collect(Collectors.toSet())
                )
                .addDirective(
                        new Directive()
                                .setName(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", resolvedReferenceTypeDeclaration.getQualifiedName())
                                .addArgument("exists", true)
                                .addArgument("grpcClassName", graphQLConfig.getGrpcEnumTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(resolvedReferenceTypeDeclaration.getClassName())))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }

    protected String findTypeName(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        if (resolvedReferenceTypeDeclaration instanceof JavaParserClassDeclaration) {
            return ((JavaParserClassDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Name.class)
                    .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                    .filter(name -> !Strings.isNullOrEmpty(name))
                    .orElseGet(() -> ((JavaParserClassDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Type.class)
                            .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                            .filter(name -> !Strings.isNullOrEmpty(name))
                            .orElse(resolvedReferenceTypeDeclaration.getName()));
        } else if (resolvedReferenceTypeDeclaration instanceof JavaParserInterfaceDeclaration) {
            return ((JavaParserInterfaceDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Name.class)
                    .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                    .filter(name -> !Strings.isNullOrEmpty(name))
                    .orElseGet(() -> ((JavaParserInterfaceDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Interface.class)
                            .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                            .filter(name -> !Strings.isNullOrEmpty(name))
                            .orElse(resolvedReferenceTypeDeclaration.getName()));
        } else if (resolvedReferenceTypeDeclaration instanceof JavaParserEnumDeclaration) {
            return ((JavaParserEnumDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Name.class)
                    .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                    .filter(name -> !Strings.isNullOrEmpty(name))
                    .orElseGet(() -> ((JavaParserEnumDeclaration) resolvedReferenceTypeDeclaration).getWrappedNode().getAnnotationByClass(org.eclipse.microprofile.graphql.Enum.class)
                            .flatMap(annotationExpr -> findAnnotationValue(annotationExpr).map(expression -> expression.asStringLiteralExpr().getValue()))
                            .filter(name -> !Strings.isNullOrEmpty(name))
                            .orElse(resolvedReferenceTypeDeclaration.getName()));
        }
        return resolvedReferenceTypeDeclaration.getName();
    }

    protected Optional<Expression> findAnnotationValue(AnnotationExpr annotationExpr) {
        if (annotationExpr.isSingleMemberAnnotationExpr()) {
            return Optional.of(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue());
        } else if (annotationExpr.isNormalAnnotationExpr()) {
            return annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                    .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                    .findFirst()
                    .map(MemberValuePair::getValue);
        }
        return Optional.empty();
    }

    protected Field buildField(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        return new Field(getInvokeFieldName(resolvedMethodDeclaration.getName()))
                .setType(getInvokeFieldTypeName(resolvedMethodDeclaration.getReturnType()));
    }

    protected EnumValue buildEnumValue(ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration) {
        return new EnumValue(getInvokeFieldName(resolvedEnumConstantDeclaration.getName()));
    }

    private Optional<String> getSourceNameFromMethodDeclaration(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().stream()
                .filter(parameter -> parameter.isAnnotationPresent(Source.class))
                .flatMap(parameter -> parameter.getAnnotationByClass(Source.class).stream())
                .findFirst()
                .flatMap(annotationExpr -> {
                            if (annotationExpr.isNormalAnnotationExpr()) {
                                return annotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                        .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                                        .filter(memberValuePair -> Strings.isNullOrEmpty(memberValuePair.getValue().asStringLiteralExpr().getValue()))
                                        .findFirst();
                            }
                            return Optional.empty();
                        }
                )
                .map(memberValuePair -> memberValuePair.getValue().asStringLiteralExpr().getValue());
    }

    private Optional<String> getQueryNameFromMethodDeclaration(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getAnnotationByClass(Query.class).stream()
                .findFirst()
                .flatMap(this::findAnnotationValue)
                .map(expression -> expression.asStringLiteralExpr().getValue());
    }

    private Optional<String> getMutationNameFromMethodDeclaration(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getAnnotationByClass(Mutation.class).stream()
                .findFirst()
                .flatMap(this::findAnnotationValue)
                .map(expression -> expression.asStringLiteralExpr().getValue());
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

    private String getInvokeFieldTypeName(Type type) {
        if (type.isArrayType()) {
            return "[".concat(getInvokeFieldTypeName(type.asArrayType().getComponentType())).concat("]");
        } else if (type.isPrimitiveType()) {
            if (type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.SHORT) ||
                    type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.INT) ||
                    type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.LONG)) {
                return "Int";
            } else if (type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.FLOAT) ||
                    type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.DOUBLE)) {
                return "Float";
            } else if (type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.CHAR) ||
                    type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.BYTE)) {
                return "String";
            } else if (type.asPrimitiveType().getType().equals(PrimitiveType.Primitive.BOOLEAN)) {
                return "Boolean";
            }
        } else if (type.isReferenceType()) {
            try {
                ResolvedReferenceType resolvedReferenceType = type.resolve().asReferenceType();
                return getInvokeFieldTypeName(resolvedReferenceType);
            } catch (UnsolvedSymbolException e) {
                return getInvokeFieldTypeName(type.toString());
            }
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(type.toString()));
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

    private String getInvokeFieldArgumentTypeName(Type type) {
        String invokeFieldTypeName = getInvokeFieldTypeName(type);
        if (manager.isObject(invokeFieldTypeName)) {
            return invokeFieldTypeName.concat("Input");
        }
        return invokeFieldTypeName;
    }

    private String getInvokeFieldTypeName(ResolvedType resolvedType) {
        if (resolvedType.isArray()) {
            return "[".concat(getInvokeFieldTypeName(resolvedType.asArrayType().getComponentType())).concat("]");
        } else if (resolvedType.isPrimitive()) {
            return getInvokeFieldTypeName(resolvedType.asPrimitive());
        } else if (resolvedType.isReferenceType()) {
            return getInvokeFieldTypeName(resolvedType.asReferenceType());
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(resolvedType.toString()));
    }

    private String getInvokeFieldTypeName(ResolvedPrimitiveType resolvedPrimitiveType) {
        if (resolvedPrimitiveType.in(SHORT, INT, LONG)) {
            return "Int";
        } else if (resolvedPrimitiveType.in(FLOAT, DOUBLE)) {
            return "Float";
        } else if (resolvedPrimitiveType.in(BYTE, CHAR)) {
            return "String";
        } else if (resolvedPrimitiveType.isBoolean()) {
            return "Boolean";
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(resolvedPrimitiveType.toString()));
    }

    private String getInvokeFieldTypeName(ResolvedReferenceType resolvedReferenceType) {
        if (resolvedReferenceType.getQualifiedName().equals(PublisherBuilder.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Mono.class.getCanonicalName())) {
            return getInvokeFieldTypeName(resolvedReferenceType.typeParametersValues().get(0).asReferenceType());
        } else if (resolvedReferenceType.getQualifiedName().equals(Collection.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(List.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Set.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Flux.class.getCanonicalName())) {
            return "[".concat(getInvokeFieldTypeName(resolvedReferenceType.typeParametersValues().get(0).asReferenceType())).concat("]");
        } else if (resolvedReferenceType.getQualifiedName().equals(Integer.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Short.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Byte.class.getCanonicalName())) {
            return "Int";
        } else if (resolvedReferenceType.getQualifiedName().equals(Float.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Double.class.getCanonicalName())) {
            return "Float";
        } else if (resolvedReferenceType.getQualifiedName().equals(String.class.getCanonicalName()) ||
                resolvedReferenceType.getQualifiedName().equals(Character.class.getCanonicalName())) {
            return "String";
        } else if (resolvedReferenceType.getQualifiedName().equals(Boolean.class.getCanonicalName())) {
            return "Boolean";
        } else if (resolvedReferenceType.getQualifiedName().equals(BigInteger.class.getCanonicalName())) {
            return "BigInteger";
        } else if (resolvedReferenceType.getQualifiedName().equals(BigDecimal.class.getCanonicalName())) {
            return "BigDecimal";
        } else if (resolvedReferenceType.getQualifiedName().equals(LocalDate.class.getCanonicalName())) {
            return "Date";
        } else if (resolvedReferenceType.getQualifiedName().equals(LocalTime.class.getCanonicalName())) {
            return "Time";
        } else if (resolvedReferenceType.getQualifiedName().equals(LocalDateTime.class.getCanonicalName())) {
            return "DateTime";
        } else {
            return resolvedReferenceType.getTypeDeclaration().map(this::findTypeName)
                    .orElseGet(() -> resolvedReferenceType.getQualifiedName().substring(resolvedReferenceType.getQualifiedName().lastIndexOf(".") + 1));
        }
    }
}
