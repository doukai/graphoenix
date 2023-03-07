package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.QueryDataLoader;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.FetchHandler;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

@ApplicationScoped
public class QueryDataLoaderBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private Map<String, Map<String, Map<String, Set<String>>>> fetchTypeMap;

    @Inject
    public QueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public QueryDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("QueryDataLoader build success");
    }

    private JavaFile buildClass() {
        fetchTypeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isFetchField)
                .map(fieldDefinitionContext ->
                        new AbstractMap.SimpleEntry<>(
                                manager.getPackageName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                new AbstractMap.SimpleEntry<>(
                                        manager.getProtocol(fieldDefinitionContext),
                                        new AbstractMap.SimpleEntry<>(
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getTo(fieldDefinitionContext)
                                        )
                                )
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>>::getValue,
                                        Collectors.groupingBy(
                                                Map.Entry<String, AbstractMap.SimpleEntry<String, String>>::getKey,
                                                Collectors.mapping(
                                                        Map.Entry<String, AbstractMap.SimpleEntry<String, String>>::getValue,
                                                        Collectors.groupingBy(
                                                                Map.Entry<String, String>::getKey,
                                                                Collectors.mapping(
                                                                        Map.Entry<String, String>::getValue,
                                                                        Collectors.toSet()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                );

        TypeSpec typeSpec = buildQueryDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildQueryDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("QueryDataLoaderImpl")
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(QueryDataLoader.class))
                .addAnnotation(Dependent.class)
                .addMethod(buildConstructor())
                .addMethod(buildDispatchMethod());

        fetchTypeMap.entrySet().stream()
                .flatMap(packageEntry ->
                        packageEntry.getValue().keySet().stream()
                                .map(protocol -> Tuple.of(packageEntry.getKey(), protocol))
                )
                .forEach(protocol ->
                        builder.addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(Mono.class, String.class),
                                        String.join(
                                                "_",
                                                TYPE_NAME_UTIL.packageNameToUnderline(protocol._1()),
                                                protocol._2(),
                                                "JsonMono"
                                        ),
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                );
        return builder.build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class);

        fetchTypeMap.entrySet().stream()
                .flatMap(packageEntry ->
                        packageEntry.getValue().keySet().stream()
                                .map(protocol -> Tuple.of(packageEntry.getKey(), protocol))
                )
                .forEach(protocol ->
                        builder.addStatement("this.$L = build($S, $S).flatMap(operation -> $T.get($T.class, $S).operation($S, operation.toString()))",
                                String.join(
                                        "_",
                                        TYPE_NAME_UTIL.packageNameToUnderline(protocol._1()),
                                        protocol._2(),
                                        "JsonMono"
                                ),
                                protocol._1(),
                                protocol._2(),
                                ClassName.get(BeanContext.class),
                                ClassName.get(FetchHandler.class),
                                protocol._2(),
                                protocol._1()
                        )
                );
        return builder.build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (Tuple2<String, String> protocol : fetchTypeMap.entrySet().stream()
                .flatMap(packageEntry ->
                        packageEntry.getValue().keySet().stream()
                                .map(protocol -> Tuple.of(packageEntry.getKey(), protocol))
                ).collect(Collectors.toList())) {
            if (index == 0) {
                monoList.add(
                        CodeBlock.of(
                                "return this.$L.doOnNext(response -> addResult($S, $S, response))",
                                String.join(
                                        "_",
                                        TYPE_NAME_UTIL.packageNameToUnderline(protocol._1()),
                                        protocol._2(),
                                        "JsonMono"
                                ),
                                protocol._1(),
                                protocol._2()
                        )
                );
            } else {
                monoList.add(
                        CodeBlock.of(
                                ".then(this.$L.doOnNext(response -> addResult($S, $S, response)))",
                                String.join(
                                        "_",
                                        TYPE_NAME_UTIL.packageNameToUnderline(protocol._1()),
                                        protocol._2(),
                                        "JsonMono"
                                ),
                                protocol._1(),
                                protocol._2()
                        )
                );
            }
            index++;
        }
        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            monoList.add(
                    CodeBlock.of(
                            ".thenReturn(dispatch(jsonValue.asJsonObject()))",
                            ClassName.get(Mono.class)
                    )
            );
            codeBlock = CodeBlock.join(monoList, System.lineSeparator());
        } else {
            codeBlock = CodeBlock.of("return Mono.empty()");
        }
        return MethodSpec.methodBuilder("load")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addParameter(ParameterSpec.builder(ClassName.get(JsonValue.class), "jsonValue").build())
                .addStatement(codeBlock)
                .build();
    }
}
