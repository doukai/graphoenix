package io.graphoenix.protobuf.builder;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.protobuf.builder.v3.Enum;
import io.graphoenix.protobuf.builder.v3.Field;
import io.graphoenix.protobuf.builder.v3.Message;
import io.graphoenix.protobuf.builder.v3.ProtoFile;
import io.graphoenix.protobuf.builder.v3.Rpc;
import io.graphoenix.protobuf.builder.v3.Service;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
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

    public String buildProto3() {
        return new ProtoFile()
                .setPkg(graphQLConfig.getPackageName())
                .setTopLevelDefs(
                        Streams.concat(
                                        manager.getObjects().map(this::buildMessage).map(Message::toString),
                                        manager.getInputObjects().map(this::buildMessage).map(Message::toString),
                                        manager.getEnums().map(this::buildEnum).map(Enum::toString),
                                        buildQueryRpcRequest().map(Message::toString),
                                        buildQueryRpcResponse().map(Message::toString),
                                        buildMutationRpcRequest().map(Message::toString),
                                        buildMutationRpcResponse().map(Message::toString),
                                        buildQueryService().stream().map(Service::toString),
                                        buildMutationService().stream().map(Service::toString)
                                )
                                .collect(Collectors.toList())
                )
                .toString();
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
                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                .flatMap(argumentsDefinitionContext ->
                                                        IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size() - 1)
                                                                .mapToObj(index ->
                                                                        new Field()
                                                                                .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                .setNumber(index + 1)
                                                                )
                                                )
                                                .collect(Collectors.toList())
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
                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                .flatMap(argumentsDefinitionContext ->
                                                        IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size() - 1)
                                                                .mapToObj(index ->
                                                                        new Field()
                                                                                .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                .setNumber(index + 1)
                                                                )
                                                )
                                                .collect(Collectors.toList())
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
        return new Enum(enumTypeDefinitionContext);
    }

    public Message buildMessage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Message()
                .setName(objectTypeDefinitionContext.name().getText())
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
                .setName(inputObjectTypeDefinitionContext.name().getText())
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
            return fieldTypeName;
        }
    }

    public String getMessageFiledName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(getMessageFiledName(fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }

    public String getServiceRpcName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(getServiceRpcName(fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
    }
}
