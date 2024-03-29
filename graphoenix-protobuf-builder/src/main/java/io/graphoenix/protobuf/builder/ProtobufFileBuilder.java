package io.graphoenix.protobuf.builder;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class ProtobufFileBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public ProtobufFileBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.graphQLConfig = graphQLConfig;
    }

    public Map<String, String> buildProto3() {
        Map<String, String> protoFileMap = new HashMap<>();

        protoFileMap.put("objects", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getObjects().filter(manager::isNotOperationType).filter(packageManager::isOwnPackage).flatMap(objectTypeDefinitionContext -> getImportPath(objectTypeDefinitionContext.fieldsDefinition())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getObjects().filter(manager::isNotOperationType).filter(packageManager::isOwnPackage).flatMap(objectTypeDefinitionContext -> getImportScalarTypePath(objectTypeDefinitionContext.fieldsDefinition())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        Arrays.asList(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getGrpcObjectTypePackageName())
                        )
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(manager.getObjects().filter(manager::isNotOperationType).filter(packageManager::isOwnPackage).map(this::buildMessage).map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("interfaces", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getInterfaces().filter(packageManager::isOwnPackage).flatMap(interfaceTypeDefinitionContext -> getImportPath(interfaceTypeDefinitionContext.fieldsDefinition())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getInterfaces().filter(packageManager::isOwnPackage).flatMap(interfaceTypeDefinitionContext -> getImportScalarTypePath(interfaceTypeDefinitionContext.fieldsDefinition())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        Arrays.asList(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getGrpcObjectTypePackageName())
                        )
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(manager.getInterfaces().filter(packageManager::isOwnPackage).map(this::buildMessage).map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("input_objects", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getInputObjects().filter(packageManager::isOwnPackage).flatMap(inputObjectTypeDefinitionContext -> getImportPath(inputObjectTypeDefinitionContext.inputObjectValueDefinitions())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(manager.getInputObjects().filter(packageManager::isOwnPackage).flatMap(inputObjectTypeDefinitionContext -> getImportScalarTypePath(inputObjectTypeDefinitionContext.inputObjectValueDefinitions())))
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        Arrays.asList(
                                new Option().setName("java_multiple_files").setValue(true),
                                new Option().setName("java_package").setValue(graphQLConfig.getGrpcInputObjectTypePackageName())
                        )
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(manager.getInputObjects().filter(packageManager::isOwnPackage).map(this::buildMessage).map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("enums", new ProtoFile()
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcEnumTypePackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(manager.getEnums().filter(packageManager::isOwnPackage).map(this::buildEnum).map(Enum::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query_requests", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getQueryOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext -> getImportPath(objectTypeDefinitionContext.fieldsDefinition()))
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getQueryOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext ->
                                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                .flatMap(fieldDefinitionContext ->
                                                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                )
                                                )
                                                .flatMap(this::getImportPath)
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        Stream.concat(
                                                manager.getQueryOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext -> getImportScalarTypePath(objectTypeDefinitionContext.fieldsDefinition())),
                                                manager.getQueryOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext ->
                                                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                        .flatMap(fieldDefinitionContext ->
                                                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                        )
                                                        )
                                                        .flatMap(this::getImportScalarTypePath)
                                        )
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildQueryRpcRequest().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query_responses", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getQueryOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext -> getImportPath(objectTypeDefinitionContext.fieldsDefinition()))
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getQueryOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext ->
                                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                .flatMap(fieldDefinitionContext ->
                                                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                )
                                                )
                                                .flatMap(this::getImportPath)
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        Stream.concat(
                                                manager.getQueryOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext -> getImportScalarTypePath(objectTypeDefinitionContext.fieldsDefinition())),
                                                manager.getQueryOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext ->
                                                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                        .flatMap(fieldDefinitionContext ->
                                                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                        )
                                                        )
                                                        .flatMap(this::getImportScalarTypePath)
                                        )
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildQueryRpcResponse().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation_requests", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getMutationOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext -> getImportPath(objectTypeDefinitionContext.fieldsDefinition()))
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getMutationOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext ->
                                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                .flatMap(fieldDefinitionContext ->
                                                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                )
                                                )
                                                .flatMap(this::getImportPath)
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        Stream.concat(
                                                manager.getMutationOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext -> getImportScalarTypePath(objectTypeDefinitionContext.fieldsDefinition())),
                                                manager.getMutationOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext ->
                                                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                        .flatMap(fieldDefinitionContext ->
                                                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                        )
                                                        )
                                                        .flatMap(this::getImportScalarTypePath)
                                        )
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildMutationRpcRequest().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation_responses", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto"))
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getMutationOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext -> getImportPath(objectTypeDefinitionContext.fieldsDefinition()))
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        manager.getMutationOperationTypeName()
                                                .flatMap(manager::getObject).stream()
                                                .flatMap(objectTypeDefinitionContext ->
                                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                .flatMap(fieldDefinitionContext ->
                                                                        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                )
                                                )
                                                .flatMap(this::getImportPath)
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .addImports(
                        io.vavr.collection.List
                                .ofAll(
                                        Stream.concat(
                                                manager.getMutationOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext -> getImportScalarTypePath(objectTypeDefinitionContext.fieldsDefinition())),
                                                manager.getMutationOperationTypeName()
                                                        .flatMap(manager::getObject).stream()
                                                        .flatMap(objectTypeDefinitionContext ->
                                                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                                        .flatMap(fieldDefinitionContext ->
                                                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                                        )
                                                        )
                                                        .flatMap(this::getImportScalarTypePath)
                                        )
                                )
                                .distinctBy(Import::getName)
                                .toJavaList()
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildMutationRpcResponse().map(Message::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("query", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto")),
                        new Import().setName(getPath("query_requests.proto")),
                        new Import().setName(getPath("query_responses.proto"))
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildQueryService().stream().map(Service::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("mutation", new ProtoFile()
                .setImports(
                        new Import().setName(getPath("enums.proto")),
                        new Import().setName(getPath("objects.proto")),
                        new Import().setName(getPath("interfaces.proto")),
                        new Import().setName(getPath("input_objects.proto")),
                        new Import().setName(getPath("mutation_requests.proto")),
                        new Import().setName(getPath("mutation_responses.proto"))
                )
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .setTopLevelDefs(buildMutationService().stream().map(Service::toString).collect(Collectors.toList()))
                .toString()
        );
        protoFileMap.put("graphql", new ProtoFile()
                .setOptions(
                        new Option().setName("java_multiple_files").setValue(true),
                        new Option().setName("java_package").setValue(graphQLConfig.getGrpcPackageName())
                )
                .setPkg(graphQLConfig.getGrpcPackageName())
                .addTopLevelDef(buildGraphQLService().toString())
                .addTopLevelDef(buildGraphQLRpcRequest().toString())
                .addTopLevelDef(buildGraphQLRpcResponse().toString())
                .toString()
        );
        return protoFileMap;
    }

    public Optional<Service> buildQueryService() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .map(objectTypeDefinitionContext ->
                        new Service().setName(objectTypeDefinitionContext.name().getText() + "Service")
                                .setRpcs(
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .filter(packageManager::isOwnPackage)
                                                .map(fieldDefinitionContext ->
                                                        new Rpc()
                                                                .setName(getServiceRpcName(fieldDefinitionContext.name().getText()))
                                                                .setMessageType("Query" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Request")
                                                                .setReturnType("Query" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Response")
                                                                .setDescription(Optional.ofNullable(fieldDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                )
                                                .collect(Collectors.toList())
                                )
                                .setDescription(Optional.ofNullable(objectTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                );
    }

    public Optional<Service> buildMutationService() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
                .map(objectTypeDefinitionContext ->
                        new Service().setName(objectTypeDefinitionContext.name().getText() + "Service")
                                .setRpcs(
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .filter(packageManager::isOwnPackage)
                                                .map(fieldDefinitionContext ->
                                                        new Rpc()
                                                                .setName(getServiceRpcName(fieldDefinitionContext.name().getText()))
                                                                .setMessageType("Mutation" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Request")
                                                                .setReturnType("Mutation" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Response")
                                                                .setDescription(Optional.ofNullable(fieldDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                )
                                                .collect(Collectors.toList())
                                )
                                .setDescription(Optional.ofNullable(objectTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                );
    }

    public Service buildGraphQLService() {
        return new Service().setName("GraphQLService")
                .addRpc(
                        new Rpc()
                                .setName("Request")
                                .setMessageType("GraphQLRequest")
                                .setReturnType("GraphQLResponse")
                );
    }

    public Stream<Message> buildQueryRpcRequest() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(packageManager::isOwnPackage)
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Query" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Request")
                                .setFields(
                                        Stream.concat(
                                                Stream.of(new Field().setName("selection_set").setOptional(true).setType("string").setNumber(1), new Field().setName("arguments").setOptional(true).setType("string").setNumber(2)),
                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                        .flatMap(argumentsDefinitionContext ->
                                                                IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size())
                                                                        .mapToObj(index ->
                                                                                new Field()
                                                                                        .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                        .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setOptional(argumentsDefinitionContext.inputValueDefinition().get(index).type().nonNullType() == null)
                                                                                        .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setNumber(index + 3)
                                                                                        .setDescription(Optional.ofNullable(argumentsDefinitionContext.inputValueDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
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
                .filter(packageManager::isOwnPackage)
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Query" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Response")
                                .addField(
                                        new Field()
                                                .setName(getMessageFiledName(fieldDefinitionContext.name().getText()))
                                                .setType(buildType(fieldDefinitionContext.type()))
                                                .setOptional(fieldDefinitionContext.type().nonNullType() == null)
                                                .setRepeated(manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                .setNumber(1)

                                )
                );
    }

    public Stream<Message> buildMutationRpcRequest() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(packageManager::isOwnPackage)
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Mutation" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Request")
                                .setFields(
                                        Stream.concat(
                                                Stream.of(
                                                        new Field().setName("selection_set").setOptional(true).setType("string").setNumber(1), new Field().setName("arguments").setOptional(true).setType("string").setNumber(2)
                                                ),
                                                Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                                                        .flatMap(argumentsDefinitionContext ->
                                                                IntStream.range(0, argumentsDefinitionContext.inputValueDefinition().size())
                                                                        .mapToObj(index ->
                                                                                new Field()
                                                                                        .setName(getMessageFiledName(argumentsDefinitionContext.inputValueDefinition().get(index).name().getText()))
                                                                                        .setType(buildType(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setOptional(argumentsDefinitionContext.inputValueDefinition().get(index).type().nonNullType() == null)
                                                                                        .setRepeated(manager.fieldTypeIsList(argumentsDefinitionContext.inputValueDefinition().get(index).type()))
                                                                                        .setNumber(index + 3)
                                                                                        .setDescription(Optional.ofNullable(argumentsDefinitionContext.inputValueDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
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
                .filter(packageManager::isOwnPackage)
                .map(fieldDefinitionContext ->
                        new Message()
                                .setName("Mutation" + getServiceRpcName(fieldDefinitionContext.name().getText()) + "Response")
                                .addField(
                                        new Field()
                                                .setName(getMessageFiledName(fieldDefinitionContext.name().getText()))
                                                .setType(buildType(fieldDefinitionContext.type()))
                                                .setOptional(fieldDefinitionContext.type().nonNullType() == null)
                                                .setRepeated(manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                .setNumber(1)
                                )
                );
    }

    public Message buildGraphQLRpcRequest() {
        return new Message()
                .setName("GraphQLRequest")
                .addField(new Field().setName("request").setType("string").setNumber(1))
                .addField(new Field().setName("transaction_id").setType("string").setOptional(true).setNumber(2));
    }

    public Message buildGraphQLRpcResponse() {
        return new Message()
                .setName("GraphQLResponse")
                .addField(new Field().setName("response").setType("string").setNumber(1));
    }

    public Enum buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new Enum()
                .setName(getName(enumTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().size())
                                .mapToObj(index ->
                                        new EnumField()
                                                .setName(
                                                        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().get(index).enumValue().enumValueName().getText() +
                                                                "_" +
                                                                getEnumFieldName(enumTypeDefinitionContext.name().getText())
                                                )
                                                .setDescription(Optional.ofNullable(enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                .setNumber(index)
                                )
                                .collect(Collectors.toList())
                )
                .setDescription(Optional.ofNullable(enumTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null));
    }

    public Message buildMessage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Message()
                .setName(getName(objectTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().size())
                                .mapToObj(index ->
                                        new Field()
                                                .setName(getMessageFiledName(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).name().getText()))
                                                .setType(buildType(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setOptional(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type().nonNullType() == null)
                                                .setRepeated(manager.fieldTypeIsList(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setDescription(Optional.ofNullable(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                .setNumber(index + 1)
                                )
                                .collect(Collectors.toList())
                )
                .setDescription(Optional.ofNullable(objectTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null));
    }

    public Message buildMessage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return new Message()
                .setName(getName(interfaceTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().size())
                                .mapToObj(index ->
                                        new Field()
                                                .setName(getMessageFiledName(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).name().getText()))
                                                .setType(buildType(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setOptional(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type().nonNullType() == null)
                                                .setRepeated(manager.fieldTypeIsList(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).type()))
                                                .setDescription(Optional.ofNullable(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                .setNumber(index + 1)
                                )
                                .collect(Collectors.toList())
                )
                .setDescription(Optional.ofNullable(interfaceTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null));
    }

    public Message buildMessage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return new Message()
                .setName(getName(inputObjectTypeDefinitionContext.name().getText()))
                .setFields(
                        IntStream.range(0, inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().size())
                                .mapToObj(index ->
                                        new Field()
                                                .setName(getMessageFiledName(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).name().getText()))
                                                .setType(buildType(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).type()))
                                                .setOptional(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).type().nonNullType() == null)
                                                .setRepeated(manager.fieldTypeIsList(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).type()))
                                                .setDescription(Optional.ofNullable(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().get(index).description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null))
                                                .setNumber(index + 1)
                                )
                                .collect(Collectors.toList())
                )
                .setDescription(Optional.ofNullable(inputObjectTypeDefinitionContext.description()).map(descriptionContext -> DOCUMENT_UTIL.getStringValue(descriptionContext.StringValue())).orElse(null));
    }

    public String buildType(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                    return "string";
                case "Date":
                    return "google.type.Date";
                case "Time":
                    return "google.type.TimeOfDay";
                case "DateTime":
                    return "google.type.DateTime";
                case "Timestamp":
                    return "google.protobuf.Timestamp";
                case "Boolean":
                    return "bool";
                case "Int":
                    return "int32";
                case "Float":
                    return "float";
                case "BigInteger":
                case "BigDecimal":
                    return "google.type.Decimal";
                default:
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
            }
        } else if (manager.isEnum(fieldTypeName)) {
            return manager.getEnum(fieldTypeName)
                    .filter(packageManager::isNotOwnPackage)
                    .flatMap(enumTypeDefinitionContext -> manager.getGrpcPackageName(enumTypeDefinitionContext).map(packageName -> packageName + "." + getName(fieldTypeName)))
                    .orElseGet(() -> getName(fieldTypeName));
        } else if (manager.isObject(fieldTypeName)) {
            return manager.getObject(fieldTypeName)
                    .filter(packageManager::isNotOwnPackage)
                    .flatMap(objectTypeDefinitionContext -> manager.getGrpcPackageName(objectTypeDefinitionContext).map(packageName -> packageName + "." + getName(fieldTypeName)))
                    .orElseGet(() -> getName(fieldTypeName));
        } else if (manager.isInterface(fieldTypeName)) {
            return manager.getInterface(fieldTypeName)
                    .filter(packageManager::isNotOwnPackage)
                    .flatMap(interfaceTypeDefinitionContext -> manager.getGrpcPackageName(interfaceTypeDefinitionContext).map(packageName -> packageName + "." + getName(fieldTypeName)))
                    .orElseGet(() -> getName(fieldTypeName));
        } else if (manager.isInputObject(fieldTypeName)) {
            return manager.getInputObject(fieldTypeName)
                    .filter(packageManager::isNotOwnPackage)
                    .flatMap(inputObjectTypeDefinitionContext -> manager.getGrpcPackageName(inputObjectTypeDefinitionContext).map(packageName -> packageName + "." + getName(fieldTypeName)))
                    .orElseGet(() -> getName(fieldTypeName));
        } else {
            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
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
            return "intro_" + getMessageFiledName(fieldName.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }

    public String getServiceRpcName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro" + getServiceRpcName(fieldName.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
    }

    public String getEnumFieldName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "INTRO_" + getEnumFieldName(fieldName.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
    }

    public String getPath(String protoName) {
        return graphQLConfig.getPackageName().replaceAll("\\.", "/") + "/" + protoName;
    }

    public String getPath(String packageName, String protoName) {
        return packageName.replaceAll("\\.", "/") + "/" + protoName;
    }

    private Stream<Import> getImportPath(GraphqlParser.FieldsDefinitionContext fieldsDefinitionContext) {
        return Streams.concat(
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isEnum(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "enums.proto"))
                        .map(path -> new Import().setName(path)),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isObject(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "objects.proto"))
                        .map(path -> new Import().setName(path)),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isInterface(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "interfaces.proto"))
                        .map(path -> new Import().setName(path))
        );
    }

    private Stream<Import> getImportPath(GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext) {
        return Streams.concat(
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isEnum(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "enums.proto"))
                        .map(path -> new Import().setName(path)),
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isInputObject(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "input_objects.proto"))
                        .map(path -> new Import().setName(path))
        );
    }

    private Stream<Import> getImportPath(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return Streams.concat(
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isEnum(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "enums.proto"))
                        .map(path -> new Import().setName(path)),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(packageManager::isNotOwnPackage)
                        .filter(typeContext -> manager.isInputObject(manager.getFieldTypeName(typeContext)))
                        .map(manager::getPackageName)
                        .flatMap(Optional::stream)
                        .map(packageName -> getPath(packageName, "input_objects.proto"))
                        .map(path -> new Import().setName(path))
        );
    }

    private Stream<Import> getImportScalarTypePath(GraphqlParser.FieldsDefinitionContext fieldsDefinitionContext) {
        return Streams.concat(
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Date"))
                        .map(path -> new Import().setName("google/type/date.proto")),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Time"))
                        .map(path -> new Import().setName("google/type/timeofday.proto")),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("DateTime"))
                        .map(path -> new Import().setName("google/type/datetime.proto")),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Timestamp"))
                        .map(path -> new Import().setName("google/protobuf/timestamp.proto")),
                fieldsDefinitionContext.fieldDefinition().stream()
                        .filter(packageManager::isOwnPackage)
                        .map(GraphqlParser.FieldDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("BigInteger") || manager.getFieldTypeName(typeContext).equals("BigDecimal"))
                        .map(path -> new Import().setName("google/type/decimal.proto"))
        );
    }

    private Stream<Import> getImportScalarTypePath(GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext) {
        return Streams.concat(
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Date"))
                        .map(path -> new Import().setName("google/type/date.proto")),
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Time"))
                        .map(path -> new Import().setName("google/type/timeofday.proto")),
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("DateTime"))
                        .map(path -> new Import().setName("google/type/datetime.proto")),
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Timestamp"))
                        .map(path -> new Import().setName("google/protobuf/timestamp.proto")),
                inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("BigInteger") || manager.getFieldTypeName(typeContext).equals("BigDecimal"))
                        .map(path -> new Import().setName("google/type/decimal.proto"))
        );
    }

    private Stream<Import> getImportScalarTypePath(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return Streams.concat(
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Date"))
                        .map(path -> new Import().setName("google/type/date.proto")),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Time"))
                        .map(path -> new Import().setName("google/type/timeofday.proto")),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("DateTime"))
                        .map(path -> new Import().setName("google/type/datetime.proto")),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("Timestamp"))
                        .map(path -> new Import().setName("google/protobuf/timestamp.proto")),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .map(GraphqlParser.InputValueDefinitionContext::type)
                        .filter(typeContext -> manager.isScalar(manager.getFieldTypeName(typeContext)))
                        .filter(typeContext -> manager.getFieldTypeName(typeContext).equals("BigInteger") || manager.getFieldTypeName(typeContext).equals("BigDecimal"))
                        .map(path -> new Import().setName("google/type/decimal.proto"))
        );
    }
}
