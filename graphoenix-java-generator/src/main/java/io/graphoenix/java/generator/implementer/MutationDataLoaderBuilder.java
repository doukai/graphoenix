package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.MutationDataLoader;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;

@ApplicationScoped
public class MutationDataLoaderBuilder {

    private final IGraphQLDocumentManager manager;
    private final GraphQLConfig graphQLConfig;
    private Map<String, Map<String, Set<Tuple2<String, String>>>> fetchTypeMap;

    @Inject
    public MutationDataLoaderBuilder(IGraphQLDocumentManager manager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("MutationDataLoader build success");
    }

    private JavaFile buildClass() {
        this.fetchTypeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isFetchField)
                .map(fieldDefinitionContext ->
                        new AbstractMap.SimpleEntry<>(
                                manager.getPackageName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                new AbstractMap.SimpleEntry<>(
                                        manager.getProtocol(fieldDefinitionContext),
                                        Tuple.of(
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getObjectTypeIDFieldName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                        .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()))))
                                        )
                                )
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, AbstractMap.SimpleEntry<String, Tuple2<String, String>>>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, AbstractMap.SimpleEntry<String, Tuple2<String, String>>>::getValue,
                                        Collectors.groupingBy(
                                                Map.Entry<String, Tuple2<String, String>>::getKey,
                                                Collectors.mapping(
                                                        AbstractMap.SimpleEntry<String, Tuple2<String, String>>::getValue,
                                                        Collectors.toSet()
                                                )
                                        )
                                )
                        )
                );
        TypeSpec typeSpec = buildMutationDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildMutationDataLoader() {

        return TypeSpec.classBuilder("MutationDataLoaderImpl")
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(MutationDataLoader.class))
                .addAnnotation(Dependent.class)
                .addMethod(buildDispatchMethod())
                .addMethod(buildLoadMethod())
                .build();
    }

    private MethodSpec buildDispatchMethod() {

        List<CodeBlock> monoList = fetchTypeMap.entrySet().stream()
                .flatMap(packageEntry ->
                        packageEntry.getValue().keySet().stream()
                                .map(protocol -> Tuple.of(packageEntry.getKey(), protocol))
                )
                .map(protocol ->
                        CodeBlock.of("fetch($S, $S)",
                                protocol._1(),
                                protocol._2()
                        )
                )
                .collect(Collectors.toList());

        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            codeBlock = CodeBlock.of("return $T.concat($L).collectList().map(list -> dispatch(jsonValue.asJsonObject()))", ClassName.get(Flux.class), CodeBlock.join(monoList, ","));
        } else {
            codeBlock = CodeBlock.of("return $T.just(jsonValue)", ClassName.get(Mono.class));
        }
        return MethodSpec.methodBuilder("load")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(JsonValue.class), "jsonValue").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addStatement(codeBlock)
                .build();
    }

    private MethodSpec buildLoadMethod() {

        List<CodeBlock> monoList = fetchTypeMap.entrySet().stream()
                .flatMap(packageEntry ->
                        packageEntry.getValue().keySet().stream()
                                .map(protocol -> Tuple.of(packageEntry.getKey(), protocol))
                )
                .map(protocol ->
                        CodeBlock.of("fetch($S, $S)",
                                protocol._1(),
                                protocol._2()
                        )
                )
                .collect(Collectors.toList());

        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            codeBlock = CodeBlock.of("return $T.concat($L).collectList().then()", ClassName.get(Flux.class), CodeBlock.join(monoList, ","));
        } else {
            codeBlock = CodeBlock.of("return $T.empty()", ClassName.get(Mono.class));
        }
        return MethodSpec.methodBuilder("load")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Void.class)))
                .addStatement(codeBlock)
                .build();
    }
}
