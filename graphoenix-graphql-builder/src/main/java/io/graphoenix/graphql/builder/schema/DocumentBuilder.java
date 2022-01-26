package io.graphoenix.graphql.builder.schema;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
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
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.META_INTERFACE_NAME;

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

    public Document buildDocument() throws IOException {

        manager.getObjects().map(objectTypeDefinitionContext -> buildObject(objectTypeDefinitionContext, true)).forEach(objectType -> manager.registerGraphQL(objectType.toString()));
        buildObjectExpressions().forEach(inputObjectType -> manager.registerGraphQL(inputObjectType.toString()));

        ObjectType queryType = new ObjectType().setName("QueryType").addFields(buildQueryTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        ObjectType mutationType = new ObjectType().setName("MutationType").addFields(buildMutationTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        manager.registerGraphQL(queryType.toString());
        manager.registerGraphQL(mutationType.toString());
        manager.registerGraphQL(new Schema().setQuery(queryType.getName()).setMutation(mutationType.getName()).toString());

        return getDocument();
    }

    public Document getDocument() {
        return new Document()
                .addDefinition(buildSchema().toString())
                .addDefinitions(manager.getScalars().map(this::buildScalarType).map(ScalarType::toString).collect(Collectors.toSet()))
                .addDefinitions(manager.getEnums().map(this::buildEnum).map(EnumType::toString).collect(Collectors.toSet()))
                .addDefinitions(manager.getInterfaces().map(this::buildInterface).map(InterfaceType::toString).collect(Collectors.toSet()))
                .addDefinitions(manager.getObjects().map(this::buildObject).map(ObjectType::toString).collect(Collectors.toSet()))
                .addDefinitions(manager.getInputObjects().map(this::buildInputObjectType).map(InputObjectType::toString).collect(Collectors.toSet()))
                //TODO union type
                .addDefinitions(manager.getDirectives().map(this::buildDirectiveDefinition).map(DirectiveDefinition::toString).collect(Collectors.toSet()));
    }

    public Schema buildSchema() {
        return new Schema()
                .setQuery(manager.getQueryOperationTypeName().orElseThrow())
                .setMutation(manager.getMutationOperationTypeName().orElseThrow());
    }

    public ScalarType buildScalarType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {

        return new ScalarType()
                .setName(scalarTypeDefinitionContext.name().getText())
                .setDescription(scalarTypeDefinitionContext.description() == null ? null : scalarTypeDefinitionContext.description().getText())
                .setDirectives(scalarTypeDefinitionContext.directives() == null ? null : scalarTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }


    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return buildObject(objectTypeDefinitionContext, false);
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean buildInterface) {

        ObjectType objectType = new ObjectType()
                .setName(objectTypeDefinitionContext.name().getText())
                .setDescription(objectTypeDefinitionContext.description() == null ? null : objectTypeDefinitionContext.description().getText())
                .setInterfaces(objectTypeDefinitionContext.implementsInterfaces() == null ? null : objectTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toSet()))
                .setFields(objectTypeDefinitionContext.fieldsDefinition() == null ? null : objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))).collect(Collectors.toSet()))
                .setDirectives(objectTypeDefinitionContext.directives() == null ? null : objectTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));

        if (buildInterface) {
            objectType.addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());
        }
        return objectType;
    }

    public InputObjectType buildInputObjectType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return new InputObjectType()
                .setName(inputObjectTypeDefinitionContext.name().getText())
                .setDescription(inputObjectTypeDefinitionContext.description() == null ? null : inputObjectTypeDefinitionContext.description().getText())
                .setInputValues(inputObjectTypeDefinitionContext.inputObjectValueDefinitions() == null ? null : inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toSet()))
                .setDirectives(inputObjectTypeDefinitionContext.directives() == null ? null : inputObjectTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }

    public InterfaceType buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {

        return new InterfaceType()
                .setName(interfaceTypeDefinitionContext.name().getText())
                .setDescription(interfaceTypeDefinitionContext.description() == null ? null : interfaceTypeDefinitionContext.description().getText())
                .setInterfaces(interfaceTypeDefinitionContext.implementsInterfaces() == null ? null : interfaceTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toSet()))
                .setFields(interfaceTypeDefinitionContext.fieldsDefinition() == null ? null : interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, false)).collect(Collectors.toSet()))
                .setDirectives(interfaceTypeDefinitionContext.directives() == null ? null : interfaceTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }

    public List<Field> getMetaInterfaceFields() {
        return manager.getInterface(META_INTERFACE_NAME).orElseThrow()
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, false))
                .collect(Collectors.toList());
    }

    public EnumType buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new EnumType()
                .setName(enumTypeDefinitionContext.name().getText())
                .setDescription(enumTypeDefinitionContext.description() == null ? null : enumTypeDefinitionContext.description().getText())
                .setEnumValues(enumTypeDefinitionContext.enumValueDefinitions() == null ? null : enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream().map(this::buildEnumValue).collect(Collectors.toSet()))
                .setDirectives(enumTypeDefinitionContext.directives() == null ? null : enumTypeDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }

    public Field buildFiled(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean isMutationOperationType) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText())
                .setDescription(fieldDefinitionContext.description() == null ? null : fieldDefinitionContext.description().getText())
                .setTypeName(fieldDefinitionContext.type().getText())
                .setDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> filedObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        if (filedObjectTypeDefinitionContext.isPresent()) {
            if (isMutationOperationType) {
                field.addArguments(buildArgumentsFromObjectType(filedObjectTypeDefinitionContext.get(), InputType.INPUT));
            } else {
                field.addArguments(buildArgumentsFromObjectType(filedObjectTypeDefinitionContext.get(), InputType.EXPRESSION));
            }
        } else if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            field.addArgument(filedToArgument(fieldDefinitionContext, InputType.EXPRESSION));
        }
        return field;
    }

    public EnumValue buildEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        return new EnumValue().setName(enumValueDefinitionContext.enumValue().enumValueName().getText())
                .setDescription(enumValueDefinitionContext.description() == null ? null : enumValueDefinitionContext.description().getText())
                .setDirectives(enumValueDefinitionContext.directives() == null ? null : enumValueDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }

    public InputValue buildInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new InputValue().setName(inputValueDefinitionContext.name().getText())
                .setDefaultValue(inputValueDefinitionContext.defaultValue() == null ? null : inputValueDefinitionContext.defaultValue().value().getText())
                .setDescription(inputValueDefinitionContext.description() == null ? null : inputValueDefinitionContext.description().getText())
                .setTypeName(inputValueDefinitionContext.type().getText())
                .setDirectives(inputValueDefinitionContext.directives() == null ? null : inputValueDefinitionContext.directives().directive().stream().map(this::buildDirective).map(Directive::toString).collect(Collectors.toSet()));
    }

    public DirectiveDefinition buildDirectiveDefinition(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        return new DirectiveDefinition().setName(directiveDefinitionContext.name().getText())
                .setDescription(directiveDefinitionContext.description() == null ? null : directiveDefinitionContext.description().getText())
                .setArguments(directiveDefinitionContext.argumentsDefinition() == null ? null : directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toSet()))
                .setDirectiveLocations(directiveDefinitionContext.directiveLocations() == null ? null : directiveLocationList(directiveDefinitionContext.directiveLocations()));
    }

    public Set<String> directiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        Set<String> directiveLocationList = new HashSet<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(directiveLocationsContext.directiveLocation().name().getText());
        } else if (directiveLocationsContext.directiveLocations() != null) {
            directiveLocationList.addAll(directiveLocationList(directiveLocationsContext.directiveLocations()));
        }
        return directiveLocationList;
    }

    public List<Field> buildQueryTypeFields() {
        return manager.getObjects()
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildSchemaTypeField(objectTypeDefinitionContext, InputType.EXPRESSION),
                                buildSchemaTypeFieldList(objectTypeDefinitionContext, InputType.EXPRESSION)
                        )
                )
                .collect(Collectors.toList());
    }

    public List<Field> buildMutationTypeFields() {
        return manager.getObjects()
                .map(objectTypeDefinitionContext -> buildSchemaTypeField(objectTypeDefinitionContext, InputType.INPUT))
                .collect(Collectors.toList());
    }

    public Field buildSchemaTypeField(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        return new Field().setName(getSchemaFieldName(objectTypeDefinitionContext))
                .setTypeName(objectTypeDefinitionContext.name().getText())
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType));
    }

    public Field buildSchemaTypeFieldList(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        return new Field().setName(getSchemaFieldName(objectTypeDefinitionContext).concat("List"))
                .setTypeName("[".concat(objectTypeDefinitionContext.name().getText()).concat("]"))
                .addArguments(buildArgumentsFromObjectType(objectTypeDefinitionContext, inputType));
    }

    private String getSchemaFieldName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        if (objectTypeDefinitionContext.name().getText().startsWith("__")) {
            return "__".concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText().replace("__", "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
        }
    }

    public Set<InputValue> buildArgumentsFromObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        return objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> filedToArgument(fieldDefinitionContext, inputType))
                .collect(Collectors.toSet());
    }

    public InputValue filedToArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, InputType inputType) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (inputType.equals(InputType.INPUT)) {
            return new InputValue().setName(fieldDefinitionContext.name().getText())
                    .setTypeName(fieldDefinitionContext.type().getText()
                            .replace(fieldTypeName, fieldTypeName.concat(manager.isObject(fieldTypeName) ? InputType.INPUT.toString() : "")));
        } else {
            if (fieldDefinitionContext.name().getText().equals("isDeprecated")) {
                return new InputValue().setName("includeDeprecated")
                        .setTypeName("Boolean")
                        .setDefaultValue("false");
            }
            return new InputValue().setName(fieldDefinitionContext.name().getText())
                    .setTypeName(fieldTypeName.concat(fieldTypeName.equals("Boolean") ? "" : InputType.EXPRESSION.toString()));
        }
    }

    public List<InputObjectType> buildObjectExpressions() {
        return Stream.concat(
                Stream.concat(
                        manager.getObjects().map(this::objectToInput),
                        manager.getObjects().map(this::objectToExpression)
                ),
                manager.getEnums().map(this::enumToExpression)
        ).collect(Collectors.toList());
    }

    public InputObjectType objectToInput(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new InputObjectType().setName(objectTypeDefinitionContext.name().getText().concat(InputType.INPUT.toString()))
                .setInputValues(buildArgumentsFromObjectType(objectTypeDefinitionContext, InputType.INPUT));
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
        return new Directive()
                .setName(directiveContext.name().getText())
                .setArguments(directiveContext.arguments().argument().stream()
                        .map(this::buildArgument)
                        .collect(Collectors.toSet()));
    }

    public Argument buildArgument(GraphqlParser.ArgumentContext argumentContext) {
        return new Argument()
                .setName(argumentContext.name().getText())
                .setValueWithVariable(argumentContext.valueWithVariable());
    }

    enum InputType {
        EXPRESSION("Expression"), INPUT("Input");

        private final String express;

        InputType(String express) {
            this.express = express;
        }

        @Override
        public String toString() {
            return express;
        }
    }
}
