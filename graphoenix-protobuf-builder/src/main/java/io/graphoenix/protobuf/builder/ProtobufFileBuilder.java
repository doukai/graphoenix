package io.graphoenix.protobuf.builder;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.protobuf.builder.v3.Enum;
import io.graphoenix.protobuf.builder.v3.EnumField;
import io.graphoenix.protobuf.builder.v3.Field;
import io.graphoenix.protobuf.builder.v3.Import;
import io.graphoenix.protobuf.builder.v3.Message;
import io.graphoenix.protobuf.builder.v3.Option;
import io.graphoenix.protobuf.builder.v3.ProtoFile;
import io.graphoenix.protobuf.builder.v3.Rpc;
import io.graphoenix.protobuf.builder.v3.Service;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class ProtobufFileBuilder {

    private final IGraphQLDocumentManager manager;

    private GraphQLConfig graphQLConfig;

    @Inject
    public ProtobufFileBuilder(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public ProtobufFileBuilder setGraphQLConfig(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        return this;
    }

    public Map<String, String> buildProto3() {
        Map<String, String> protoFileMap = new HashMap<>();
        protoFileMap.put("objects", new ProtoFile()
                .setImports(
                        List.of(new Import().setName("enums.proto"))
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(manager.getObjects().map(this::buildMessage).map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("input_objects", new ProtoFile()
                .setImports(
                        List.of(new Import().setName("enums.proto"))
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(manager.getInputObjects().map(this::buildMessage).map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("enums", new ProtoFile()
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(manager.getEnums().map(this::buildEnum).map(Enum::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query_requests", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildQueryRpcRequest().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query_response", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildQueryRpcResponse().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation_requests", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildMutationRpcRequest().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation_response", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildMutationRpcResponse().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto"),
                                new Import().setName("query_requests.proto"),
                                new Import().setName("query_response.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildQueryService().stream().map(Service::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation", new ProtoFile()
                .setImports(
                        List.of(
                                new Import().setName("enums.proto"),
                                new Import().setName("objects.proto"),
                                new Import().setName("input_objects.proto"),
                                new Import().setName("mutation_requests.proto"),
                                new Import().setName("mutation_response.proto")
                        )
                )
                .setOptions(
                        List.of(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getPackageName())
                        )
                )
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(buildMutationService().stream().map(Service::toString).collect(Collectors.toList()))
                .toString()
        );
        return protoFileMap;
    }

    public Optional<Service> buildQueryService() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .map(objectTypeDefinitionContext ->
                        new Service().setName(objectTypeDefinitionContext.name().getText().concat("Service"))
                                .setRpcs(
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .map(fieldDefinitionContext ->
                                                        new Rpc()
                                                                .setName(getServiceRpcName(fieldDefinitionContext.name().getText()))
                                                                .setMessageType("Query".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Request"))
                                                                .setReturnType("Query".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Response"))
                                                )
                                                .collect(Collectors.toList())
                                )
                );
    }

    public Optional<Service> buildMutationService() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
                .map(objectTypeDefinitionContext ->
                        new Service().setName(objectTypeDefinitionContext.name().getText().concat("Service"))
                                .setRpcs(
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .map(fieldDefinitionContext ->
                                                        new Rpc()
                                                                .setName(getServiceRpcName(fieldDefinitionContext.name().getText()))
                                                                .setMessageType("Mutation".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Request"))
                                                                .setReturnType("Mutation".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Response"))
                                                )
                                                .collect(Collectors.toList())
                                )
                );
    }

    public Stream<Message> buildQueryRpcRequest() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Query".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Request"))
                                .setFields(
                                        Stream.concat(
                                                Stream.of(new Field().setName("selectionSet").setType("string").setNumber(1), new Field().setName("layers").setType("int32").setNumber(2)),
                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                        .flatMap(argumentsDefinitionContext ->
                                                                IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size() - 1)
                                                                        .mapToObj(index ->
                                                                                new Field()
                                                                                        .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                        .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setNumber(index + 3)
                                                                        )
                                                        )
                                        ).collect(Collectors.toList())
                                )
                );
    }

    public Stream<Message> buildQueryRpcResponse() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Query".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Response"))
                                .setFields(
                                        Collections.singletonList(
                                                new Field()
                                                        .setName(getMessageFiledName(fieldDefinitionContext.name().getText()))
                                                        .setType(buildType(fieldDefinitionContext.type()))
                                                        .setRepeated(manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                        .setNumber(1)
                                        )
                                )
                );
    }

    public Stream<Message> buildMutationRpcRequest() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Mutation".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Request"))
                                .setFields(
                                        Stream.concat(
                                                Stream.of(new Field().setName("selectionSet").setType("string").setNumber(1), new Field().setName("layers").setType("int32").setNumber(2)),
                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                        .flatMap(argumentsDefinitionContext ->
                                                                IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size() - 1)
                                                                        .mapToObj(index ->
                                                                                new Field()
                                                                                        .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                        .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setNumber(index + 3)
                                                                        )
                                                        )
                                        ).collect(Collectors.toList())
                                )
                );
    }

    public Stream<Message> buildMutationRpcResponse() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Mutation".concat(getServiceRpcName(fieldDefinitionContext.name().getText())).concat("Response"))
                                .setFields(
                                        Collections.singletonList(
                                                new Field()
                                                        .setName(getMessageFiledName(fieldDefinitionContext.name().getText()))
                                                        .setType(buildType(fieldDefinitionContext.type()))
                                                        .setRepeated(manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                        .setNumber(1)
                                        )
                                )
                );
    }

    public Enum buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new Enum()
                .setName(getName(enumTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().size() - 1)
                                .mapToObj(index ->
                                        new EnumField()
                                                .setName(
                                                        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().get(index).enumValue().enumValueName().getText()
                                                                .concat("_")
                                                                .concat(getEnumFieldName(enumTypeDefinitionContext.name().getText()))
                                                )
                                                .setNumber(index)
                                )
                                .collect(Collectors.toList())
                );
    }

    public Message buildMessage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Message()
                .setName(getName(objectTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().size() - 1)
                                .mapToObj(index ->
                                        new Field()
                                                .setName(getMessageFiledName(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).name().getText()))
                                                .setType(buildType(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setRepeated(manager.fieldTypeIsList(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setNumber(index + 1)
                                )
                                .collect(Collectors.toList())
                );
    }

    public Message buildMessage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return new Message()
                .setName(getName(inputObjectTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().size() - 1)
                                .mapToObj(index ->
                                        new Field()
                                                .setName(getMessageFiledName(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).name().getText()))
                                                .setType(buildType(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).type()))
                                                .setRepeated(manager.fieldTypeIsList(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).type()))
                                                .setNumber(index + 1)
                                )
                                .collect(Collectors.toList())
                );
    }

    public String buildType(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    return "string";
                case "Boolean":
                    return "bool";
                case "Int":
                    return "int32";
                case "Float":
                    return "float";
                case "BigInteger":
                    return "int64";
                case "BigDecimal":
                    return "double";
                default:
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
            }
        } else {
            return getName(fieldTypeName);
        }
    }

    public String getName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return fieldName.replaceFirst(INTROSPECTION_PREFIX, "Intro");
        }
        return fieldName;
    }

    public String getMessageFiledName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "intro_".concat(getMessageFiledName(fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }

    public String getServiceRpcName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(getServiceRpcName(fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
    }

    public String getEnumFieldName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "INTRO_".concat(getEnumFieldName(fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
    }
}
