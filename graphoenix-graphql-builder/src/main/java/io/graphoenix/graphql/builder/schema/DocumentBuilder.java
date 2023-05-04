package io.graphoenix.graphql.builder.schema;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.document.Directive;
import io.graphoenix.core.document.Document;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputObjectType;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.document.InterfaceType;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.document.ScalarType;
import io.graphoenix.core.document.Schema;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.core.operation.EnumValue;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class DocumentBuilder {

    private final GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    private final IGraphQLFieldMapManager mapper;

    private final GraphQLConfigRegister graphQLConfigRegister;

    private final PackageManager packageManager;

    @Inject
    public DocumentBuilder(GraphQLConfig graphQLConfig,
                           IGraphQLDocumentManager manager,
                           IGraphQLFieldMapManager mapper,
                           GraphQLConfigRegister graphQLConfigRegister,
                           PackageManager packageManager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
        this.mapper = mapper;
        this.graphQLConfigRegister = graphQLConfigRegister;
        this.packageManager = packageManager;
    }

    public void startupManager() throws IOException, URISyntaxException {
        manager.registerInputStream(getClass().getClassLoader().getResourceAsStream("META-INF/graphql/main.gql"));
        mapper.registerFieldMaps();
    }

    public void buildManager() throws IOException, URISyntaxException {
        graphQLConfigRegister.registerPackage();
        graphQLConfigRegister.registerConfig();
        if (graphQLConfig.getBuild()) {
            manager.registerGraphQL(buildDocument().toString());
        }
        mapper.registerFieldMaps();
    }

    public Document buildDocument() {
        io.vavr.collection.Stream.ofAll(
                manager.getObjects()
                        .filter(packageManager::isOwnPackage)
                        .filter(manager::isNotOperationType)
                        .filter(manager::isNotContainerType)
                        .flatMap(objectTypeDefinitionContext ->
                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                        .map(fieldDefinitionContext -> Tuple.of(objectTypeDefinitionContext, fieldDefinitionContext))
                        )
                        .filter(tuple -> manager.getFetchAnchor(tuple._2()))
                        .filter(tuple -> manager.hasFetchWith(tuple._2()))
        )
                .distinctBy(tuple -> manager.getFetchWithType(tuple._2()))
                .toJavaStream()
                .flatMap(tuple -> buildFetchWithObject(tuple._1(), tuple._2()).stream())
                .collect(Collectors.toList())
                .forEach(objectType -> manager.registerGraphQL(objectType.toString()));

        io.vavr.collection.Stream.ofAll(
                manager.getObjects()
                        .filter(packageManager::isOwnPackage)
                        .filter(manager::isNotOperationType)
                        .filter(manager::isNotContainerType)
                        .flatMap(objectTypeDefinitionContext ->
                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                        .map(fieldDefinitionContext -> Tuple.of(objectTypeDefinitionContext, fieldDefinitionContext))
                        )
                        .filter(tuple -> manager.hasMapWith(tuple._2()))
        )
                .distinctBy(tuple -> manager.getMapWithType(tuple._2()))
                .toJavaStream()
                .flatMap(tuple -> buildMapWithObject(tuple._1(), tuple._2()).stream())
                .collect(Collectors.toList())
                .forEach(objectType -> manager.registerGraphQL(objectType.toString()));

        manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .map(objectTypeDefinitionContext -> buildObject(objectTypeDefinitionContext, true, true, true))
                .forEach(objectType -> manager.registerGraphQL(objectType.toString()));

        if (manager.getObjects().anyMatch(manager::isNotContainerType)) {
            ObjectType queryType = manager.getObject(manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME))
                    .map(this::buildObject)
                    .orElseGet(() -> new ObjectType(QUERY_TYPE_NAME))
                    .addFields(buildQueryTypeFields())
                    .addInterface(META_INTERFACE_NAME)
                    .addFields(getMetaInterfaceFields());

            ObjectType mutationType = manager.getObject(manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME))
                    .map(this::buildObject)
                    .orElseGet(() -> new ObjectType(MUTATION_TYPE_NAME))
                    .addFields(buildMutationTypeFields())
                    .addInterface(META_INTERFACE_NAME)
                    .addFields(getMetaInterfaceFields());

            manager.registerGraphQL(queryType.toString());
            manager.registerGraphQL(mutationType.toString());
            manager.registerGraphQL(new Schema().setQuery(queryType.getName()).setMutation(mutationType.getName()).toString());
        }

        buildArgumentInputObjects().forEach(inputObjectType -> manager.registerGraphQL(inputObjectType.toString()));
        buildContainerTypeObjects().forEach(objectType -> manager.registerGraphQL(objectType.toString()));

        mapper.registerFieldMaps();

        Document document = getDocument();
        Logger.info("document build success");
        Logger.debug("\r\n{}", document.toString());
        return document;
    }

    public Document getDocument() {
        Document document = new Document()
                .addDefinitions(manager.getScalars().map(ScalarType::new).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getEnums().map(this::buildEnum).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getInterfaces().map(this::buildInterface).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getObjects().map(this::buildObject).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getInputObjects().map(this::buildInputObjectType).collect(Collectors.toCollection(LinkedHashSet::new)))
                //TODO union type
                .addDefinitions(manager.getDirectives().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getOperationDefinitions().map(Operation::new).collect(Collectors.toCollection(LinkedHashSet::new)));

        Optional.ofNullable(manager.getSchema())
                .map(Schema::new)
                .or(() ->
                        manager.getQueryOperationTypeName()
                                .map(queryTypeName -> new Schema().setQuery(queryTypeName).setMutation(manager.getMutationOperationTypeName().orElse(null)))
                )
                .ifPresent(document::addDefinition);

        return document;
    }

    public Document getPackageDocument() {
        Document document = new Document()
                .addDefinitions(manager.getScalars().map(ScalarType::new).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getEnums().filter(packageManager::isOwnPackage).map(this::buildEnum).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getInterfaces().filter(packageManager::isOwnPackage).map(this::buildInterface).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getObjects().filter(manager::isNotOperationType).filter(packageManager::isOwnPackage).map(this::buildObject).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(
                        manager.getObjects()
                                .filter(manager::isOperationType)
                                .map(objectTypeDefinitionContext ->
                                        buildObject(
                                                objectTypeDefinitionContext,
                                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                        .filter(packageManager::isOwnPackage)
                                                        .collect(Collectors.toList())
                                        )
                                )
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .addDefinitions(manager.getInputObjects().filter(packageManager::isOwnPackage).map(this::buildInputObjectType).collect(Collectors.toCollection(LinkedHashSet::new)))
                //TODO union type
                .addDefinitions(manager.getDirectives().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getOperationDefinitions().filter(packageManager::isOwnPackage).map(Operation::new).collect(Collectors.toCollection(LinkedHashSet::new)));

        Optional.ofNullable(manager.getSchema())
                .map(Schema::new)
                .or(() ->
                        manager.getQueryOperationTypeName()
                                .map(queryTypeName -> new Schema().setQuery(queryTypeName).setMutation(manager.getMutationOperationTypeName().orElse(null)))
                )
                .ifPresent(document::addDefinition);

        return document;
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return buildObject(objectTypeDefinitionContext, objectTypeDefinitionContext.fieldsDefinition().fieldDefinition(), false, false, false);
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        return buildObject(objectTypeDefinitionContext, fieldDefinitionContextList, false, false, false);
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean buildInterface, boolean buildArgument, boolean buildField) {
        return buildObject(objectTypeDefinitionContext, objectTypeDefinitionContext.fieldsDefinition().fieldDefinition(), buildInterface, buildArgument, buildField);
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList, boolean buildInterface, boolean buildArgument, boolean buildField) {
        ObjectType objectType = new ObjectType(objectTypeDefinitionContext);

        if (manager.getPackageName(objectTypeDefinitionContext).isEmpty()) {
            objectType.addDirective(
                    new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                            .addArgument("packageName", graphQLConfig.getPackageName())
                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
            );
        }

        if (!manager.hasClassName(objectTypeDefinitionContext)) {
            objectType.addDirective(
                    new io.graphoenix.core.operation.Directive(CLASS_INFO_DIRECTIVE_NAME)
                            .addArgument("className", graphQLConfig.getObjectTypePackageName().concat(".").concat(objectTypeDefinitionContext.name().getText()))
                            .addArgument("grpcClassName", graphQLConfig.getGrpcObjectTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(objectTypeDefinitionContext.name().getText())))
            );
        }

        if (buildArgument) {
            objectType.setFields(
                    fieldDefinitionContextList.stream()
                            .map(fieldDefinitionContext ->
                                    manager.isNotInvokeField(fieldDefinitionContext) ?
                                            buildField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext, manager.isMutationOperationType(objectTypeDefinitionContext.name().getText())) :
                                            buildField(fieldDefinitionContext)
                            )
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }

        if (buildInterface) {
            objectType.addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields().stream().filter(metaField -> objectType.getFields().stream().noneMatch(field -> field.getName().equals(metaField.getName()))).collect(Collectors.toList()));
        }

        if (buildField) {
            objectType.addField(buildTypeNameField(objectTypeDefinitionContext))
                    .addFields(buildFunctionFieldList(objectTypeDefinitionContext))
                    .addFields(
                            fieldDefinitionContextList.stream()
                                    .filter(manager::isNotInvokeField)
                                    .filter(manager::isNotFetchField)
                                    .filter(manager::isNotFunctionField)
                                    .filter(manager::isNotConnectionField)
                                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().equals("__typename"))
                                    .filter(this::isNotMetaInterfaceField)
                                    .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                    .flatMap(fieldDefinitionContext ->
                                            Stream.of(
                                                    buildListObjectAggregateField(fieldDefinitionContext),
                                                    buildListObjectConnectionField(fieldDefinitionContext)
                                            )
                                    )
                                    .collect(Collectors.toList())
                    )
                    .addFields(
                            fieldDefinitionContextList.stream()
                                    .filter(manager::isFetchField)
                                    .filter(manager::getFetchAnchor)
                                    .filter(fieldDefinitionContext -> !manager.hasFetchWith(fieldDefinitionContext))
                                    .map(fieldDefinitionContext -> new Field(manager.getFetchFrom(fieldDefinitionContext)).setTypeName(getMapFieldType(manager.getFieldTypeName(fieldDefinitionContext.type()), manager.getFetchTo(fieldDefinitionContext))))
                                    .filter(field -> fieldDefinitionContextList.stream().noneMatch(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(field.getName())))
                                    .collect(Collectors.toList())
                    )
                    .addFields(
                            fieldDefinitionContextList.stream()
                                    .filter(manager::hasFetchWith)
                                    .flatMap(fieldDefinitionContext -> {
                                                String withType = manager.getFetchWithType(fieldDefinitionContext);
                                                String protocol = manager.getProtocol(fieldDefinitionContext);
                                                Field field = new Field(getSchemaFieldName(withType))
                                                        .setTypeName(manager.fieldTypeIsList(fieldDefinitionContext.type()) ? "[" + withType + "]" : withType)
                                                        .addDirective(
                                                                manager.getFetchAnchor(fieldDefinitionContext) ?
                                                                        new io.graphoenix.core.operation.Directive(MAP_DIRECTIVE_NAME)
                                                                                .addArgument("from", manager.getFetchFrom(fieldDefinitionContext))
                                                                                .addArgument("to", manager.getFetchWithFrom(fieldDefinitionContext)) :
                                                                        new io.graphoenix.core.operation.Directive(FETCH_DIRECTIVE_NAME)
                                                                                .addArgument("from", manager.getFetchFrom(fieldDefinitionContext))
                                                                                .addArgument("to", manager.getFetchWithFrom(fieldDefinitionContext))
                                                                                .addArgument("protocol", EnumValue.forName(protocol))
                                                        );
                                                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                                    return Stream.of(
                                                            field,
                                                            buildListObjectAggregateField(field, withType),
                                                            buildListObjectConnectionField(field, withType)
                                                    );
                                                } else {
                                                    return Stream.of(field);
                                                }
                                            }
                                    )
                                    .collect(Collectors.toList())
                    )
                    .addFields(
                            fieldDefinitionContextList.stream()
                                    .filter(manager::isMapField)
                                    .filter(manager::getMapAnchor)
                                    .filter(fieldDefinitionContext -> !manager.hasMapWith(fieldDefinitionContext))
                                    .map(fieldDefinitionContext -> new Field(manager.getMapFrom(fieldDefinitionContext)).setTypeName(getMapFieldType(manager.getFieldTypeName(fieldDefinitionContext.type()), manager.getMapTo(fieldDefinitionContext))))
                                    .filter(field -> fieldDefinitionContextList.stream().noneMatch(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(field.getName())))
                                    .collect(Collectors.toList())
                    )
                    .addFields(
                            fieldDefinitionContextList.stream()
                                    .filter(manager::hasMapWith)
                                    .flatMap(fieldDefinitionContext -> {
                                                String withType = manager.getMapWithType(fieldDefinitionContext);
                                                Field field = new Field(getSchemaFieldName(withType))
                                                        .setTypeName(manager.fieldTypeIsList(fieldDefinitionContext.type()) ? "[" + withType + "]" : withType)
                                                        .addDirective(
                                                                new io.graphoenix.core.operation.Directive(MAP_DIRECTIVE_NAME)
                                                                        .addArgument("from", manager.getMapFrom(fieldDefinitionContext))
                                                                        .addArgument("to", manager.getMapWithFrom(fieldDefinitionContext))
                                                        );
                                                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                                    return Stream.of(
                                                            field,
                                                            buildListObjectAggregateField(field, withType),
                                                            buildListObjectConnectionField(field, withType)
                                                    );
                                                } else {
                                                    return Stream.of(field);
                                                }
                                            }
                                    )
                                    .collect(Collectors.toList())
                    );
        }
        return objectType;
    }

    public Optional<ObjectType> buildFetchWithObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fetchWithTypeName = manager.getFetchWithType(fieldDefinitionContext);
        if (manager.getObject(fetchWithTypeName).isPresent()) {
            return Optional.empty();
        }
        String fetchFromTypeName = objectTypeDefinitionContext.name().getText();
        String fetchToTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fetchFromFieldName = manager.getFetchFrom(fieldDefinitionContext);
        String fetchToFieldName = manager.getFetchTo(fieldDefinitionContext);
        String fetchWithFromFieldName = manager.getFetchWithFrom(fieldDefinitionContext);
        String fetchWithToFieldName = manager.getFetchWithTo(fieldDefinitionContext);
        String protocol = manager.getProtocol(fieldDefinitionContext);

        String fetchWithFromObjectFieldName = "from";
        String fetchWithToObjectFieldName = "to";

        ObjectType objectType = new ObjectType(fetchWithTypeName)
                .addField(
                        new Field("id")
                                .setTypeName("ID")
                                .addDirective(
                                        new io.graphoenix.core.operation.Directive("dataType")
                                                .addArgument("type", "Int")
                                                .addArgument("autoIncrement", true)
                                )
                )
                .addField(new Field(fetchWithFromFieldName).setTypeName(getMapFieldType(fetchFromTypeName, manager.getFetchFrom(fieldDefinitionContext))))
                .addField(
                        new Field(fetchWithFromObjectFieldName)
                                .setTypeName(fetchFromTypeName)
                                .addDirective(
                                        manager.getFetchAnchor(fieldDefinitionContext) ?
                                                new io.graphoenix.core.operation.Directive(MAP_DIRECTIVE_NAME)
                                                        .addArgument("from", fetchWithFromFieldName)
                                                        .addArgument("to", fetchFromFieldName)
                                                        .addArgument("anchor", true) :
                                                new io.graphoenix.core.operation.Directive(FETCH_DIRECTIVE_NAME)
                                                        .addArgument("from", fetchWithFromFieldName)
                                                        .addArgument("to", fetchFromFieldName)
                                                        .addArgument("anchor", true)
                                                        .addArgument("protocol", EnumValue.forName(protocol))
                                )
                );

        if (fetchToFieldName != null) {
            objectType
                    .addField(new Field(fetchWithToFieldName).setTypeName(getMapFieldType(fetchToTypeName, manager.getFetchTo(fieldDefinitionContext))))
                    .addField(
                            new Field(fetchWithToObjectFieldName)
                                    .setTypeName(fetchToTypeName)
                                    .addDirective(
                                            new io.graphoenix.core.operation.Directive(FETCH_DIRECTIVE_NAME)
                                                    .addArgument("from", fetchWithToFieldName)
                                                    .addArgument("to", fetchToFieldName)
                                                    .addArgument("anchor", true)
                                                    .addArgument("protocol", EnumValue.forName(protocol))
                                    )
                    );
        } else {
            objectType
                    .addField(new Field(fetchWithToFieldName).setTypeName(fetchToTypeName));
        }
        return Optional.of(objectType);
    }

    public Optional<ObjectType> buildMapWithObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String mapWithTypeName = manager.getMapWithType(fieldDefinitionContext);
        if (manager.getObject(mapWithTypeName).isPresent()) {
            return Optional.empty();
        }
        String mapFromTypeName = objectTypeDefinitionContext.name().getText();
        String mapToTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String mapFromFieldName = manager.getMapFrom(fieldDefinitionContext);
        String mapToFieldName = manager.getMapTo(fieldDefinitionContext);
        String mapWithFromFieldName = manager.getMapWithFrom(fieldDefinitionContext);
        String mapWithToFieldName = manager.getMapWithTo(fieldDefinitionContext);

        String mapWithFromObjectFieldName = "from";
        String mapWithToObjectFieldName = "to";

        ObjectType objectType = new ObjectType(mapWithTypeName)
                .addField(
                        new Field("id")
                                .setTypeName("ID")
                                .addDirective(
                                        new io.graphoenix.core.operation.Directive("dataType")
                                                .addArgument("type", "Int")
                                                .addArgument("autoIncrement", true)
                                )
                )
                .addField(new Field(mapWithFromFieldName).setTypeName(getMapFieldType(mapFromTypeName, manager.getMapFrom(fieldDefinitionContext))))
                .addField(
                        new Field(mapWithFromObjectFieldName)
                                .setTypeName(mapFromTypeName)
                                .addDirective(
                                        new io.graphoenix.core.operation.Directive(MAP_DIRECTIVE_NAME)
                                                .addArgument("from", mapWithFromFieldName)
                                                .addArgument("to", mapFromFieldName)
                                                .addArgument("anchor", true)
                                )
                );

        if (mapToFieldName != null) {
            objectType
                    .addField(new Field(mapWithToFieldName).setTypeName(getMapFieldType(mapToTypeName, manager.getMapTo(fieldDefinitionContext))))
                    .addField(
                            new Field(mapWithToObjectFieldName)
                                    .setTypeName(mapToTypeName)
                                    .addDirective(
                                            new io.graphoenix.core.operation.Directive(MAP_DIRECTIVE_NAME)
                                                    .addArgument("from", mapWithToFieldName)
                                                    .addArgument("to", mapToFieldName)
                                                    .addArgument("anchor", true)
                                    )
                    );
        } else {
            objectType
                    .addField(new Field(mapWithToFieldName).setTypeName(mapToTypeName));
        }
        return Optional.of(objectType);
    }

    public InputObjectType buildInputObjectType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        InputObjectType inputObjectType = new InputObjectType(inputObjectTypeDefinitionContext);

        if (manager.getPackageName(inputObjectTypeDefinitionContext).isEmpty()) {
            inputObjectType.addDirective(
                    new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                            .addArgument("packageName", graphQLConfig.getPackageName())
                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
            );
        }

        if (!manager.hasClassName(inputObjectTypeDefinitionContext)) {
            inputObjectType.addDirective(
                    new io.graphoenix.core.operation.Directive(CLASS_INFO_DIRECTIVE_NAME)
                            .addArgument("className", graphQLConfig.getInputObjectTypePackageName().concat(".").concat(inputObjectTypeDefinitionContext.name().getText()))
                            .addArgument("annotationName", graphQLConfig.getAnnotationPackageName().concat(".").concat(inputObjectTypeDefinitionContext.name().getText()))
                            .addArgument("grpcClassName", graphQLConfig.getGrpcInputObjectTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(inputObjectTypeDefinitionContext.name().getText())))
            );
        }

        return inputObjectType;
    }

    public InterfaceType buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        InterfaceType interfaceType = new InterfaceType(interfaceTypeDefinitionContext);

        if (manager.getPackageName(interfaceTypeDefinitionContext).isEmpty()) {
            interfaceType.addDirective(
                    new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                            .addArgument("packageName", graphQLConfig.getPackageName())
                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
            );
        }

        if (!manager.hasClassName(interfaceTypeDefinitionContext)) {
            interfaceType.addDirective(
                    new io.graphoenix.core.operation.Directive(CLASS_INFO_DIRECTIVE_NAME)
                            .addArgument("className", graphQLConfig.getInterfaceTypePackageName().concat(".").concat(interfaceTypeDefinitionContext.name().getText()))
                            .addArgument("grpcClassName", graphQLConfig.getGrpcInterfaceTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(interfaceTypeDefinitionContext.name().getText())))
            );
        }

        return interfaceType;
    }

    public List<Field> getMetaInterfaceFields() {
        return manager.getInterface(META_INTERFACE_NAME).stream()
                .flatMap(interfaceTypeDefinitionContext ->
                        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                .map(fieldDefinitionContext -> buildField(interfaceTypeDefinitionContext.name().getText(), fieldDefinitionContext, false))
                )
                .collect(Collectors.toList());
    }

    public boolean isMetaInterfaceField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return manager.getInterface(META_INTERFACE_NAME).stream()
                .flatMap(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .anyMatch(item -> item.name().getText().equals(fieldDefinitionContext.name().getText()));
    }

    public boolean isNotMetaInterfaceField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return !isMetaInterfaceField(fieldDefinitionContext);
    }

    public EnumType buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        EnumType enumType = new EnumType(enumTypeDefinitionContext);

        if (manager.getPackageName(enumTypeDefinitionContext).isEmpty()) {
            enumType.addDirective(
                    new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                            .addArgument("packageName", graphQLConfig.getPackageName())
                            .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
            );
        }

        if (!manager.hasClassName(enumTypeDefinitionContext)) {
            enumType.addDirective(
                    new io.graphoenix.core.operation.Directive(CLASS_INFO_DIRECTIVE_NAME)
                            .addArgument("className", graphQLConfig.getEnumTypePackageName().concat(".").concat(enumTypeDefinitionContext.name().getText()))
                            .addArgument("grpcClassName", graphQLConfig.getGrpcEnumTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(enumTypeDefinitionContext.name().getText())))
            );
        }
        return enumType;
    }

    public Field buildField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Field(fieldDefinitionContext);
    }

    public Field buildField(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean isMutationOperationType) {
        Field field = buildField(fieldDefinitionContext);

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        if (fieldObjectTypeDefinitionContext.isPresent()) {
            if (isMutationOperationType) {
                field.addArguments(buildArgumentsFromObjectType(fieldObjectTypeDefinitionContext.get(), InputType.INPUT));
            } else {
                field.addArguments(buildArgumentsFromObjectType(fieldObjectTypeDefinitionContext.get(), InputType.EXPRESSION));
                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                    field.addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                            .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                            .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

                    manager.getFieldByDirective(typeName, CURSOR_DIRECTIVE_NAME)
                            .findFirst()
                            .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                            .ifPresent(cursorFieldDefinitionContext ->
                                    field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                            .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                            );

                    field.addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()).concat(InputType.ORDER_BY.toString())))
                            .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));
                }
            }
        } else if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                field.addArgument(new InputValue().setName("opr").setTypeName("Operator").setDefaultValue("EQ"))
                        .addArgument(new InputValue().setName("val").setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type())))
                        .addArgument(new InputValue().setName("in").setTypeName("[".concat(manager.getFieldTypeName(fieldDefinitionContext.type())).concat("]")))
                        .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                        .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                        .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

                mapper.getWithObjectTypeDefinition(typeName, fieldDefinitionContext.name().getText())
                        .flatMap(objectTypeDefinitionContext -> manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), CURSOR_DIRECTIVE_NAME)
                                .findFirst()
                                .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                        )
                        .ifPresent(cursorFieldDefinitionContext ->
                                field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                        .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                        );

                field.addArgument(new InputValue().setName(SORT_INPUT_NAME).setTypeName("Sort"));
            }
        }
        return field;
    }

    public Field buildListObjectAggregateField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText().concat(AGGREGATE_SUFFIX))
                .setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                .setDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(this::buildDirective).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()).concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildListObjectAggregateField(Field original, String withTypeName) {
        Field field = new Field().setName(original.getName().concat(AGGREGATE_SUFFIX))
                .setTypeName(withTypeName)
                .setStringDirectives(original.getDirectives())
                .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(withTypeName.concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(withTypeName);
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildListObjectConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText().concat(CONNECTION_SUFFIX))
                .setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()).concat(CONNECTION_SUFFIX))
                .addDirective(new io.graphoenix.core.operation.Directive()
                        .setName(CONNECTION_DIRECTIVE_NAME)
                        .addArgument("field", fieldDefinitionContext.name().getText())
                        .addArgument("agg", fieldDefinitionContext.name().getText().concat(AGGREGATE_SUFFIX))
                )
                .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()).concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildListObjectConnectionField(Field original, String withTypeName) {
        Field field = new Field().setName(original.getName().concat(CONNECTION_SUFFIX))
                .setTypeName(withTypeName.concat(CONNECTION_SUFFIX))
                .addDirective(new io.graphoenix.core.operation.Directive()
                        .setName(CONNECTION_DIRECTIVE_NAME)
                        .addArgument("field", original.getName())
                        .addArgument("agg", original.getName().concat(AGGREGATE_SUFFIX))
                )
                .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(withTypeName.concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(withTypeName);
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildTypeNameField(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Field().setName("__typename")
                .setTypeName("String")
                .setArguments(new LinkedHashSet<>())
                .addDirective(
                        new io.graphoenix.core.operation.Directive()
                                .setName("dataType")
                                .addArgument(
                                        "default",
                                        objectTypeDefinitionContext.name().getText()
                                )
                );
    }

    public List<Field> buildQueryTypeFields() {
        return manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildSchemaTypeField(objectTypeDefinitionContext, InputType.EXPRESSION),
                                buildSchemaTypeFieldList(objectTypeDefinitionContext, InputType.EXPRESSION),
                                buildSchemaTypeFieldConnection(objectTypeDefinitionContext, InputType.EXPRESSION)
                        )
                )
                .collect(Collectors.toList());
    }

    public List<Field> buildMutationTypeFields() {
        return manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildSchemaTypeField(objectTypeDefinitionContext, InputType.INPUT),
                                buildSchemaTypeFieldList(objectTypeDefinitionContext, InputType.INPUT)
                        )
                )
                .collect(Collectors.toList());
    }

    public Field buildSchemaTypeField(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext))
                .setTypeName(objectTypeDefinitionContext.name().getText())
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .addDirective(
                        new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                .addArgument("packageName", graphQLConfig.getPackageName())
                                .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                );
        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"));
        } else if (inputType.equals(InputType.INPUT)) {
            field.addArgument(new InputValue(WHERE_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString())));
        }
        buildSecurity(objectTypeDefinitionContext, field);
        return field;
    }

    public Field buildSchemaTypeFieldList(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext).concat("List"))
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat("]"))
                .addDirective(
                        new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                .addArgument("packageName", graphQLConfig.getPackageName())
                                .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                );

        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString())))
                    .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"))
                    .addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                    .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

            manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), CURSOR_DIRECTIVE_NAME)
                    .findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                    .ifPresent(cursorFieldDefinitionContext ->
                            field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                    .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                    );
        } else if (inputType.equals(InputType.INPUT)) {
            field.addArgument(new InputValue().setName(LIST_INPUT_NAME).setTypeName("[".concat(objectTypeDefinitionContext.name().getText().concat(inputType.toString())).concat("]")))
                    .addArgument(new InputValue(WHERE_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString())));
        }
        buildSecurity(objectTypeDefinitionContext, field);
        return field;
    }

    public Field buildSchemaTypeFieldConnection(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext).concat(CONNECTION_SUFFIX))
                .setTypeName(objectTypeDefinitionContext.name().getText().concat(CONNECTION_SUFFIX))
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"))
                .addDirective(new io.graphoenix.core.operation.Directive()
                        .setName(CONNECTION_DIRECTIVE_NAME)
                        .addArgument("field", getSchemaFieldName(objectTypeDefinitionContext).concat("List"))
                        .addArgument("agg", getSchemaFieldName(objectTypeDefinitionContext))
                )
                .addDirective(
                        new io.graphoenix.core.operation.Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                .addArgument("packageName", graphQLConfig.getPackageName())
                                .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                );

        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                    .addArgument(new InputValue().setName("exs").setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat(inputType.toString()).concat("]")))
                    .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

            manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), CURSOR_DIRECTIVE_NAME)
                    .findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                    .ifPresent(cursorFieldDefinitionContext ->
                            field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                    .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                    );
        }
        buildSecurity(objectTypeDefinitionContext, field);
        return field;
    }

    public void buildSecurity(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, Field field) {
        Stream.ofNullable(objectTypeDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext ->
                        directiveContext.name().getText().equals(PERMIT_ALL) ||
                                directiveContext.name().getText().equals(DENY_ALL) ||
                                directiveContext.name().getText().equals(ROLES_ALLOWED)
                )
                .findAny()
                .ifPresent(directiveContext -> field.addDirective(buildDirective(directiveContext)));
    }

    private String getSchemaFieldName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return getSchemaFieldName(objectTypeDefinitionContext.name().getText());
    }

    private String getSchemaFieldName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name.replace(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
        }
    }

    public Set<InputValue> buildArgumentsFromObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        if (inputType.equals(InputType.ORDER_BY)) {
            return objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                    .filter(manager::isNotInvokeField)
//                    .filter(manager::isNotFetchField)
                    .filter(manager::isNotFunctionField)
                    .filter(manager::isNotConnectionField)
                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .filter(fieldDefinitionContext -> manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .map(fieldDefinitionContext -> fieldToArgument(objectTypeDefinitionContext, fieldDefinitionContext, inputType))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            return objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                    .filter(manager::isNotInvokeField)
//                    .filter(manager::isNotFetchField)
                    .filter(manager::isNotFunctionField)
                    .filter(manager::isNotConnectionField)
                    .filter(fieldDefinitionContext -> manager.isNotContainerType(fieldDefinitionContext.type()))
                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                    .map(fieldDefinitionContext -> fieldToArgument(objectTypeDefinitionContext, fieldDefinitionContext, inputType))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public InputValue fieldToArgument(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, InputType inputType) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (inputType.equals(InputType.INPUT)) {
            if (fieldDefinitionContext.name().getText().equals("__typename")) {
                return new InputValue().setName("__typename").setTypeName("String").setDefaultValue(objectTypeDefinitionContext.name().getText());
            }
            boolean isList = manager.fieldTypeIsList(fieldDefinitionContext.type());
            return new InputValue().setName(fieldDefinitionContext.name().getText())
                    .setTypeName((isList ? "[".concat(fieldTypeName).concat("]") : fieldTypeName).replace(fieldTypeName, fieldTypeName.concat(manager.isObject(fieldTypeName) ? inputType.toString() : "")));
        } else if (inputType.equals(InputType.EXPRESSION)) {
            if (fieldDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME)) {
                return new InputValue().setName(DEPRECATED_INPUT_NAME).setTypeName("Boolean").setDefaultValue("false");
            }
            String argumentTypeName;
            switch (fieldTypeName) {
                case "Boolean":
                    argumentTypeName = "Boolean".concat(inputType.toString());
                    break;
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    argumentTypeName = "String".concat(inputType.toString());
                    break;
                case "Int":
                case "BigInteger":
                    argumentTypeName = "Int".concat(inputType.toString());
                    break;
                case "Float":
                case "BigDecimal":
                    argumentTypeName = "Float".concat(inputType.toString());
                    break;
                default:
                    argumentTypeName = fieldTypeName.concat(inputType.toString());
                    break;
            }
            return new InputValue().setName(fieldDefinitionContext.name().getText()).setTypeName(argumentTypeName);
        } else {
            return new InputValue().setName(fieldDefinitionContext.name().getText()).setTypeName("Sort");
        }
    }

    public List<InputObjectType> buildArgumentInputObjects() {
        return Streams.concat(
                manager.getObjects()
                        .filter(packageManager::isOwnPackage)
                        .filter(manager::isNotOperationType)
                        .filter(manager::isNotContainerType)
                        .flatMap(objectTypeDefinitionContext ->
                                Stream.of(
                                        objectToExpression(objectTypeDefinitionContext),
                                        objectToInput(objectTypeDefinitionContext),
                                        objectToOrderBy(objectTypeDefinitionContext)
                                )
                        ),
                manager.getEnums()
                        .filter(packageManager::isOwnPackage)
                        .map(this::enumToExpression)
        ).collect(Collectors.toList());
    }

    public List<ObjectType> buildContainerTypeObjects() {
        return manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                objectToConnection(objectTypeDefinitionContext),
                                objectToEdge(objectTypeDefinitionContext)
                        )
                )
                .collect(Collectors.toList());
    }

    public InputObjectType objectToInput(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType()
                .setName(objectTypeDefinitionContext.name().getText().concat(InputType.INPUT.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.INPUT));
    }

    public InputObjectType objectToOrderBy(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType()
                .setName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.ORDER_BY));
    }

    public ObjectType objectToConnection(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new ObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.CONNECTION.toString()))
                .addField(new Field().setName("totalCount").setTypeName("Int"))
                .addField(new Field().setName("pageInfo").setTypeName("PageInfo"))
                .addField(new Field().setName("edges").setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat(InputType.EDGE.toString()).concat("]")))
                .addDirective(new io.graphoenix.core.operation.Directive(CONTAINER_TYPE_DIRECTIVE_NAME));
    }

    public ObjectType objectToEdge(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeName = objectTypeDefinitionContext.name().getText();
        GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(typeName, CURSOR_DIRECTIVE_NAME).findFirst()
                .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

        return new ObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.EDGE.toString()))
                .addField(new Field().setName("node").setTypeName(objectTypeDefinitionContext.name().getText()))
                .addField(new Field().setName("cursor").setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                .addDirective(new io.graphoenix.core.operation.Directive(CONTAINER_TYPE_DIRECTIVE_NAME));
    }

    public InputObjectType objectToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType()
                .setName(objectTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION))
                .addInputValue(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                .addInputValue(new InputValue().setName("exs").setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat(InputType.EXPRESSION.toString()).concat("]")));
    }

    public InputObjectType enumToExpression(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new InputObjectType()
                .setName(enumTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString()))
                .addInputValue(new InputValue().setName("opr").setTypeName("Operator").setDefaultValue("EQ"))
                .addInputValue(new InputValue().setName("val").setTypeName(enumTypeDefinitionContext.name().getText()))
                .addInputValue(new InputValue().setName("in").setTypeName("[".concat(enumTypeDefinitionContext.name().getText()).concat("]")));
    }

    public io.graphoenix.core.operation.Directive buildDirective(GraphqlParser.DirectiveContext directiveContext) {
        return new io.graphoenix.core.operation.Directive(directiveContext);
    }

    public String getMapFieldType(String typeName, String filedName) {
        return manager.getField(typeName, filedName)
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if ("ID".equals(fieldTypeName)) {
                                return manager.getDataTypeName(fieldDefinitionContext).orElse("String");
                            }
                            return fieldTypeName;
                        }
                )
                .orElse("String");
    }

    public List<Field> buildFunctionFieldList(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return Stream.concat(
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(manager::isNotInvokeField)
                        .filter(manager::isNotFetchField)
                        .filter(manager::isNotFunctionField)
                        .filter(manager::isNotConnectionField)
                        .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                        .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().equals("__typename"))
                        .filter(this::isNotMetaInterfaceField)
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .filter(fieldDefinitionContext ->
                                manager.getFieldTypeName(fieldDefinitionContext.type()).equals("ID") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("String") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("Date") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("Time") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("DateTime") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("Timestamp")
                        )
                        .flatMap(fieldDefinitionContext ->
                                Stream.of(
                                        Function.COUNT.toField(
                                                fieldDefinitionContext.name().getText(),
                                                "Int",
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.MAX.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.MIN.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        )
                                )
                        ),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(manager::isNotInvokeField)
                        .filter(manager::isNotFetchField)
                        .filter(manager::isNotFunctionField)
                        .filter(manager::isNotConnectionField)
                        .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                        .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().equals("__typename"))
                        .filter(this::isNotMetaInterfaceField)
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .filter(fieldDefinitionContext ->
                                manager.getFieldTypeName(fieldDefinitionContext.type()).equals("Int") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("Float") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("BigInteger") ||
                                        manager.getFieldTypeName(fieldDefinitionContext.type()).equals("BigDecimal")
                        )
                        .flatMap(fieldDefinitionContext ->
                                Stream.of(
                                        Function.COUNT.toField(
                                                fieldDefinitionContext.name().getText(),
                                                "Int",
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.SUM.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.AVG.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.MAX.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        ),
                                        Function.MIN.toField(
                                                fieldDefinitionContext.name().getText(),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.fieldTypeIsList(fieldDefinitionContext.type())
                                        )
                                )
                        )
        ).collect(Collectors.toList());
    }

    private enum InputType {
        EXPRESSION(EXPRESSION_SUFFIX), INPUT(INPUT_SUFFIX), ORDER_BY(ORDER_BY_SUFFIX), CONNECTION(CONNECTION_SUFFIX), EDGE(EDGE_SUFFIX);

        private final String suffix;

        InputType(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String toString() {
            return suffix;
        }
    }

    private enum Function {
        COUNT("Count"),
        SUM("Sum"),
        AVG("Avg"),
        MAX("Max"),
        MIN("Min");

        private final String name;

        Function(String name) {
            this.name = name;
        }

        public Field toField(String fieldName, String returnTypeName, String fieldTypeName, boolean isList) {
            Field field = new Field()
                    .setName(fieldName.concat(name))
                    .setTypeName(returnTypeName)
                    .addDirective(
                            new io.graphoenix.core.operation.Directive()
                                    .setName(FUNC_DIRECTIVE_NAME)
                                    .addArgument("name", new EnumValue(this.name()))
                                    .addArgument("name", fieldName)
                    );

            if (isList) {
                field.addArgument(new InputValue().setName("opr").setTypeName("Operator").setDefaultValue("EQ"))
                        .addArgument(new InputValue().setName("val").setTypeName(fieldTypeName))
                        .addArgument(new InputValue().setName("in").setTypeName("[".concat(fieldTypeName).concat("]")));
            }
            return field;
        }
    }
}
