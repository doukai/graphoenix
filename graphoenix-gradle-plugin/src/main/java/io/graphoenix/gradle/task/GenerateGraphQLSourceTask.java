package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.ObjectType;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;

public class GenerateGraphQLSourceTask extends BaseTask {

    @TaskAction
    public void generateGraphQLSource() {
        final IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        final DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        final JavaFileBuilder javaFileBuilder = BeanContext.get(JavaFileBuilder.class);

        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        if (graphQLConfig == null) {
            graphQLConfig = new GraphQLConfig();
        }
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().getAsPath();

        try {
            init();
            SourceRoot sourceRoot = new SourceRoot(Path.of(javaPath));
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
                                String typeName = methodDeclaration.getParameters().stream()
                                        .filter(parameter -> parameter.isAnnotationPresent(Source.class))
                                        .findFirst()
                                        .orElseThrow()
                                        .getType()
                                        .asClassOrInterfaceType()
                                        .getNameAsString();

                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(typeName)));

                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext)
                                        .addField(
                                                new Field()
                                                        .setName(getInvokeFieldName(methodDeclaration.getNameAsString()))
                                                        .setTypeName(getInvokeFieldTypeName(methodDeclaration.getType()))
                                        );
                                manager.registerGraphQL(objectType.toString());
                            }
                    );
            javaFileBuilder.writeToPath(new File(javaPath), graphQLConfig);

        } catch (IOException | URISyntaxException e) {
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

    private String getInvokeFieldTypeName(Type type) {
        String typeName;
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType classOrInterfaceType = type.asClassOrInterfaceType();
            if (classOrInterfaceType.isAnnotationPresent(Id.class)) {
                return "ID";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(int.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(short.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(byte.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Integer.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Short.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Byte.class.getSimpleName())) {
                typeName = "Int";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(float.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(double.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Float.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Double.class.getSimpleName())) {
                typeName = "Float";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(String.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(char.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Character.class.getSimpleName())) {
                typeName = "String";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(boolean.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Boolean.class.getSimpleName())) {
                typeName = "Boolean";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(BigInteger.class.getSimpleName())) {
                typeName = "BigInteger";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(BigDecimal.class.getSimpleName())) {
                typeName = "BigDecimal";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(LocalDate.class.getSimpleName())) {
                typeName = "Date";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(LocalTime.class.getSimpleName())) {
                typeName = "Time";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(LocalDateTime.class.getSimpleName())) {
                typeName = "DateTime";
            } else if (classOrInterfaceType.getName().getIdentifier().equals(Collection.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(List.class.getSimpleName()) ||
                    classOrInterfaceType.getName().getIdentifier().equals(Set.class.getSimpleName())) {
                typeName = "[".concat(getInvokeFieldTypeName(classOrInterfaceType.getTypeArguments().orElseThrow().get(0))).concat("]");
            } else {
                typeName = classOrInterfaceType.getName().getIdentifier();
            }
            if (classOrInterfaceType.isAnnotationPresent(NonNull.class)) {
                typeName = typeName.concat("!");
            }
        } else if (type.isArrayType()) {
            typeName = "[".concat(getInvokeFieldTypeName(type.asArrayType().getElementType())).concat("]");
        } else {
            throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_FIELD_TYPE.bind(type.toString()));
        }
        return typeName;
    }
}
