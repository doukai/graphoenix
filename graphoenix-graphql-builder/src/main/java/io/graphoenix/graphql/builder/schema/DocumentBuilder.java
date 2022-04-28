package io.graphoenix.graphql.builder.schema;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.generator.document.Directive;
import io.graphoenix.graphql.generator.document.DirectiveDefinition;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.graphql.generator.document.EnumType;
import io.graphoenix.graphql.generator.document.EnumValue;
import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InputObjectType;
import io.graphoenix.graphql.generator.document.InputValue;
import io.graphoenix.graphql.generator.document.InterfaceType;
import io.graphoenix.graphql.generator.document.ObjectType;
import io.graphoenix.graphql.generator.document.ScalarType;
import io.graphoenix.graphql.generator.document.Schema;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.StringValue;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
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

import static io.graphoenix.core.error.GraphQLErrorType.META_INTERFACE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class DocumentBuilder {

    private final GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    private final IGraphQLFieldMapManager mapper;

    private final GraphQLConfigRegister graphQLConfigRegister;

    @Inject
    public DocumentBuilder(GraphQLConfig graphQLConfig,
                           IGraphQLDocumentManager manager,
                           IGraphQLFieldMapManager mapper,
                           GraphQLConfigRegister graphQLConfigRegister) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
        this.mapper = mapper;
        this.graphQLConfigRegister = graphQLConfigRegister;
    }

    public void startupManager() throws IOException, URISyntaxException {
        manager.registerInputStream(getClass().getClassLoader().getResourceAsStream("META-INF/graphql/main.gql"));
        mapper.registerFieldMaps();
    }

    public void buildManager() throws IOException, URISyntaxException {
        graphQLConfigRegister.registerPreset();
        graphQLConfigRegister.registerConfig();
        if (graphQLConfig.getBuild()) {
            manager.registerGraphQL(buildDocument().toString());
        }
        mapper.registerFieldMaps();
    }

    public Document buildDocument() throws IOException {
        manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .map(objectTypeDefinitionContext -> buildObject(objectTypeDefinitionContext, true, true, true))
                .forEach(objectType -> manager.registerGraphQL(objectType.toString()));
        buildArgumentInputObjects().forEach(inputObjectType -> manager.registerGraphQL(inputObjectType.toString()));
        buildContainerTypeObjects().forEach(objectType -> manager.registerGraphQL(objectType.toString()));

        ObjectType queryType = new ObjectType().setName("QueryType").addFields(buildQueryTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        ObjectType mutationType = new ObjectType().setName("MutationType").addFields(buildMutationTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        manager.registerGraphQL(queryType.toString());
        manager.registerGraphQL(mutationType.toString());
        manager.registerGraphQL(new Schema().setQuery(queryType.getName()).setMutation(mutationType.getName()).toString());
        mapper.registerFieldMaps();

        Document document = getDocument();
        Logger.info("document build success");
        Logger.debug("\r\n{}", document.toString());
        return document;
    }

    public Document getDocument() {
        return new Document()
                .addDefinition(buildSchema().toString())
                .addDefinitions(manager.getScalars().map(this::buildScalarType).map(ScalarType::toString).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getEnums().map(this::buildEnum).map(EnumType::toString).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getInterfaces().map(this::buildInterface).map(InterfaceType::toString).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getObjects().map(this::buildObject).map(ObjectType::toString).collect(Collectors.toCollection(LinkedHashSet::new)))
                .addDefinitions(manager.getInputObjects().map(this::buildInputObjectType).map(InputObjectType::toString).collect(Collectors.toCollection(LinkedHashSet::new)))
                //TODO union type
                .addDefinitions(manager.getDirectives().map(this::buildDirectiveDefinition).map(DirectiveDefinition::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public Schema buildSchema() {
        return new Schema()
                .setQuery(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.QUERY_TYPE_NOT_EXIST)))
                .setMutation(manager.getMutationOperationTypeName().orElse(null));
    }

    public ScalarType buildScalarType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        return new ScalarType()
                .setName(scalarTypeDefinitionContext.name().getText())
                .setDescription(scalarTypeDefinitionContext.description() == null ? null : scalarTypeDefinitionContext.description().getText())
                .setDirectives(scalarTypeDefinitionContext.directives() == null ? null : scalarTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return buildObject(objectTypeDefinitionContext, false, false, false);
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean buildInterface, boolean buildArgument, boolean buildField) {
        ObjectType objectType = new ObjectType()
                .setName(objectTypeDefinitionContext.name().getText())
                .setDescription(objectTypeDefinitionContext.description() == null ? null : objectTypeDefinitionContext.description().getText())
                .setInterfaces(objectTypeDefinitionContext.implementsInterfaces() == null ? new LinkedHashSet<>() : objectTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setFields(
                        objectTypeDefinitionContext.fieldsDefinition() == null ?
                                null :
                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                        .map(fieldDefinitionContext ->
                                                buildArgument ?
                                                        buildField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext, manager.isMutationOperationType(objectTypeDefinitionContext.name().getText())) :
                                                        buildField(fieldDefinitionContext)
                                        )
                                        .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .setStringDirectives(objectTypeDefinitionContext.directives() == null ? null : objectTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));

        if (buildInterface) {
            objectType.addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        }
        if (buildField) {
            objectType.addField(buildTypeNameField(objectTypeDefinitionContext));
            objectType.addFields(buildFunctionFieldList(objectTypeDefinitionContext));
            objectType.addFields(
                    objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                            .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                            .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                            .map(this::buildListObjectAggregateField)
                            .collect(Collectors.toList())
            );
            objectType.addFields(
                    objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                            .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                            .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                            .map(this::buildListObjectConnectionField)
                            .collect(Collectors.toList())
            );
        }
        return objectType;
    }

    public InputObjectType buildInputObjectType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return new InputObjectType()
                .setName(inputObjectTypeDefinitionContext.name().getText())
                .setDescription(inputObjectTypeDefinitionContext.description() == null ? null : inputObjectTypeDefinitionContext.description().getText())
                .setInputValues(inputObjectTypeDefinitionContext.inputObjectValueDefinitions() == null ? null : inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setDirectives(inputObjectTypeDefinitionContext.directives() == null ? null : inputObjectTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public InterfaceType buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return new InterfaceType()
                .setName(interfaceTypeDefinitionContext.name().getText())
                .setDescription(interfaceTypeDefinitionContext.description() == null ? null : interfaceTypeDefinitionContext.description().getText())
                .setInterfaces(interfaceTypeDefinitionContext.implementsInterfaces() == null ? new LinkedHashSet<>() : interfaceTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setFields(interfaceTypeDefinitionContext.fieldsDefinition() == null ? null : interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildField(interfaceTypeDefinitionContext.name().getText(), fieldDefinitionContext, false)).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setDirectives(interfaceTypeDefinitionContext.directives() == null ? null : interfaceTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public List<Field> getMetaInterfaceFields() {
        GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext = manager.getInterface(META_INTERFACE_NAME).orElseThrow(() -> new GraphQLErrors(META_INTERFACE_NOT_EXIST));

        return manager.getInterface(META_INTERFACE_NAME).orElseThrow(() -> new GraphQLErrors(META_INTERFACE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildField(interfaceTypeDefinitionContext.name().getText(), fieldDefinitionContext, false))
                .collect(Collectors.toList());
    }

    public EnumType buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new EnumType()
                .setName(enumTypeDefinitionContext.name().getText())
                .setDescription(enumTypeDefinitionContext.description() == null ? null : enumTypeDefinitionContext.description().getText())
                .setEnumValues(enumTypeDefinitionContext.enumValueDefinitions() == null ? null : enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream().map(this::buildEnumValue).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setDirectives(enumTypeDefinitionContext.directives() == null ? null : enumTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public Field buildField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Field().setName(fieldDefinitionContext.name().getText())
                .setDescription(fieldDefinitionContext.description() == null ? null : fieldDefinitionContext.description().getText())
                .setTypeName(fieldDefinitionContext.type().getText())
                .setArguments(fieldDefinitionContext.argumentsDefinition() == null ? new LinkedHashSet<>() : fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setStringDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public Field buildField(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean isMutationOperationType) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText())
                .setDescription(fieldDefinitionContext.description() == null ? null : fieldDefinitionContext.description().getText())
                .setTypeName(fieldDefinitionContext.type().getText())
                .setStringDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));

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

                    manager.getFieldByDirective(typeName, "cursor")
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
                        .flatMap(objectTypeDefinitionContext -> manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), "cursor")
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
                .setStringDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildListObjectConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText().concat(CONNECTION_SUFFIX))
                .setTypeName(manager.getFieldTypeName(fieldDefinitionContext.type()).concat(CONNECTION_SUFFIX))
                .addDirective(new Directive()
                        .setName(CONNECTION_DIRECTIVE_NAME)
                        .addArgument(new Argument().setName("field").setValueWithVariable(new StringValue(fieldDefinitionContext.name().getText())))
                        .addArgument(new Argument().setName("agg").setValueWithVariable(new StringValue(fieldDefinitionContext.name().getText().concat(AGGREGATE_SUFFIX))))
                );

        Optional<GraphqlParser.ObjectTypeDefinitionContext> fieldObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        fieldObjectTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> field.addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION)));
        return field;
    }

    public Field buildTypeNameField(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new Field().setName("__typename")
                .setTypeName("String")
                .setArguments(new LinkedHashSet<>())
                .addDirective(
                        new Directive()
                                .setName("dataType")
                                .addArgument(
                                        new Argument()
                                                .setName("default")
                                                .setValueWithVariable(new StringValue(objectTypeDefinitionContext.name().getText()))
                                )
                );
    }

    public EnumValue buildEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        return new EnumValue().setName(enumValueDefinitionContext.enumValue().enumValueName().getText())
                .setDescription(enumValueDefinitionContext.description() == null ? null : enumValueDefinitionContext.description().getText())
                .setDirectives(enumValueDefinitionContext.directives() == null ? null : enumValueDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public InputValue buildInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new InputValue().setName(inputValueDefinitionContext.name().getText())
                .setDefaultValue(inputValueDefinitionContext.defaultValue() == null ? null : inputValueDefinitionContext.defaultValue().value().getText())
                .setDescription(inputValueDefinitionContext.description() == null ? null : inputValueDefinitionContext.description().getText())
                .setTypeName(inputValueDefinitionContext.type().getText())
                .setDirectives(inputValueDefinitionContext.directives() == null ? null : inputValueDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public DirectiveDefinition buildDirectiveDefinition(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        return new DirectiveDefinition().setName(directiveDefinitionContext.name().getText())
                .setDescription(directiveDefinitionContext.description() == null ? null : directiveDefinitionContext.description().getText())
                .setArguments(directiveDefinitionContext.argumentsDefinition() == null ? new LinkedHashSet<>() : directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setDirectiveLocations(directiveDefinitionContext.directiveLocations() == null ? null : directiveLocationList(directiveDefinitionContext.directiveLocations()));
    }

    public Set<String> directiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        Set<String> directiveLocationList = new LinkedHashSet<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(directiveLocationsContext.directiveLocation().name().getText());
        } else if (directiveLocationsContext.directiveLocations() != null) {
            directiveLocationList.addAll(directiveLocationList(directiveLocationsContext.directiveLocations()));
        }
        return directiveLocationList;
    }

    public List<Field> buildQueryTypeFields() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
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
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .map(objectTypeDefinitionContext -> buildSchemaTypeField(objectTypeDefinitionContext, InputType.INPUT))
                .collect(Collectors.toList());
    }

    public Field buildSchemaTypeField(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext))
                .setTypeName(objectTypeDefinitionContext.name().getText())
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .addArgument(new InputValue("where").setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString())));

        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"));
        }
        return field;
    }

    public Field buildSchemaTypeFieldList(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext).concat("List"))
                .setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat("]"))
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"));

        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                    .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

            manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), "cursor")
                    .findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                    .ifPresent(cursorFieldDefinitionContext ->
                            field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                    .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                    );
        }
        return field;
    }

    public Field buildSchemaTypeFieldConnection(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        Field field = new Field().setName(getSchemaFieldName(objectTypeDefinitionContext).concat(CONNECTION_SUFFIX))
                .setTypeName(objectTypeDefinitionContext.name().getText().concat(CONNECTION_SUFFIX))
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType))
                .addArgument(new InputValue().setName(ORDER_BY_INPUT_NAME).setTypeName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString())))
                .addArgument(new InputValue().setName(GROUP_BY_INPUT_NAME).setTypeName("[String!]"))
                .addDirective(new Directive()
                        .setName(CONNECTION_DIRECTIVE_NAME)
                        .addArgument(new Argument().setName("field").setValueWithVariable(new StringValue(getSchemaFieldName(objectTypeDefinitionContext).concat("List"))))
                        .addArgument(new Argument().setName("agg").setValueWithVariable(new StringValue(getSchemaFieldName(objectTypeDefinitionContext))))
                );

        if (inputType.equals(InputType.EXPRESSION)) {
            field.addArgument(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                    .addArgument(new InputValue().setName(FIRST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(LAST_INPUT_NAME).setTypeName("Int"))
                    .addArgument(new InputValue().setName(OFFSET_INPUT_NAME).setTypeName("Int"));

            manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), "cursor")
                    .findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                    .ifPresent(cursorFieldDefinitionContext ->
                            field.addArgument(new InputValue().setName(AFTER_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                                    .addArgument(new InputValue().setName(BEFORE_INPUT_NAME).setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                    );
        }
        return field;
    }

    private String getSchemaFieldName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        if (objectTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText().replace(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
        }
    }

    public Set<InputValue> buildArgumentsFromObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        if (inputType.equals(InputType.ORDER_BY)) {
            return objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                    .filter(fieldDefinitionContext -> manager.isNotInvokeField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .filter(fieldDefinitionContext -> manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .map(fieldDefinitionContext -> fieldToArgument(objectTypeDefinitionContext, fieldDefinitionContext, inputType))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            return objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                    .filter(fieldDefinitionContext -> manager.isNotInvokeField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                    .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                    .map(fieldDefinitionContext -> fieldToArgument(objectTypeDefinitionContext, fieldDefinitionContext, inputType))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public InputValue fieldToArgument(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, InputType inputType) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (inputType.equals(InputType.INPUT)) {
            if (fieldDefinitionContext.name().getText().equals("__typename")) {
                return new InputValue().setName("__typename").setTypeName("String!").setDefaultValue(objectTypeDefinitionContext.name().getText());
            }
            return new InputValue().setName(fieldDefinitionContext.name().getText())
                    .setTypeName(fieldDefinitionContext.type().getText()
                            .replace(fieldTypeName, fieldTypeName.concat(manager.isObject(fieldTypeName) ? InputType.INPUT.toString() : "")));
        } else if (inputType.equals(InputType.EXPRESSION)) {
            if (fieldDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME)) {
                return new InputValue().setName(DEPRECATED_INPUT_NAME).setTypeName("Boolean").setDefaultValue("false");
            }
            return new InputValue().setName(fieldDefinitionContext.name().getText())
                    .setTypeName(fieldTypeName.concat(fieldTypeName.equals("Boolean") ? "" : InputType.EXPRESSION.toString()));
        } else {
            return new InputValue().setName(fieldDefinitionContext.name().getText()).setTypeName("Sort");
        }
    }

    public List<InputObjectType> buildArgumentInputObjects() {
        List<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContextList = manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .collect(Collectors.toList());

        return Streams.concat(
                objectTypeDefinitionContextList.stream().map(this::objectToInput),
                objectTypeDefinitionContextList.stream().map(this::objectToOrderBy),
                objectTypeDefinitionContextList.stream().map(this::objectToExpression),
                manager.getEnums().map(this::enumToExpression)
        ).collect(Collectors.toList());
    }

    public List<ObjectType> buildContainerTypeObjects() {
        List<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContextList = manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .collect(Collectors.toList());

        return Streams.concat(
                objectTypeDefinitionContextList.stream().map(this::objectToConnection),
                objectTypeDefinitionContextList.stream().map(this::objectToEdge)
        ).collect(Collectors.toList());
    }

    public InputObjectType objectToInput(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.INPUT.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.INPUT));
    }

    public InputObjectType objectToOrderBy(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.ORDER_BY.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.ORDER_BY));
    }

    public ObjectType objectToConnection(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new ObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.CONNECTION.toString()))
                .addField(new Field().setName("totalCount").setTypeName("Int"))
                .addField(new Field().setName("pageInfo").setTypeName("PageInfo!"))
                .addField(new Field().setName("edges").setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat(InputType.EDGE.toString()).concat("]")))
                .addStringDirective("containerType");
    }

    public ObjectType objectToEdge(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeName = objectTypeDefinitionContext.name().getText();
        GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(typeName, "cursor").findFirst()
                .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

        return new ObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.EDGE.toString()))
                .addField(new Field().setName("node").setTypeName(objectTypeDefinitionContext.name().getText()))
                .addField(new Field().setName("cursor").setTypeName(manager.getFieldTypeName(cursorFieldDefinitionContext.type())))
                .addStringDirective("containerType");
    }

    public InputObjectType objectToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.EXPRESSION))
                .addInputValue(new InputValue().setName("cond").setTypeName("Conditional").setDefaultValue("AND"))
                .addInputValue(new InputValue().setName("exs").setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat(InputType.EXPRESSION.toString()).concat("]")));
    }

    public InputObjectType enumToExpression(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new InputObjectType().setName(enumTypeDefinitionContext.name().getText().concat(InputType.EXPRESSION.toString()))
                .addInputValue(new InputValue().setName("opr").setTypeName("Operator").setDefaultValue("EQ"))
                .addInputValue(new InputValue().setName("val").setTypeName(enumTypeDefinitionContext.name().getText()))
                .addInputValue(new InputValue().setName("in").setTypeName("[".concat(enumTypeDefinitionContext.name().getText()).concat("]")));
    }

    public Directive buildDirective(GraphqlParser.DirectiveContext directiveContext) {
        Directive directive = new Directive()
                .setName(directiveContext.name().getText());

        if (directiveContext.arguments() != null) {
            directive.setArguments(
                    directiveContext.arguments().argument().stream()
                            .map(this::buildArgument)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return directive;
    }

    public Argument buildArgument(GraphqlParser.ArgumentContext argumentContext) {
        return new Argument()
                .setName(argumentContext.name().getText())
                .setValueWithVariable(argumentContext.valueWithVariable());
    }

    public List<Field> buildFunctionFieldList(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return Stream.concat(
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
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
                            new Directive()
                                    .setName(FUNC_DIRECTIVE_NAME)
                                    .addArgument(
                                            new Argument()
                                                    .setName("name")
                                                    .setValueWithVariable(new io.graphoenix.graphql.generator.operation.EnumValue(this.name()))
                                    )
                                    .addArgument(
                                            new Argument()
                                                    .setName("field")
                                                    .setValueWithVariable(new StringValue(fieldName))
                                    )
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
