package io.graphoenix.graphql.builder.schema;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.graphql.generator.document.Directive;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.graphql.generator.document.EnumType;
import io.graphoenix.graphql.generator.document.EnumValue;
import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InputObjectType;
import io.graphoenix.graphql.generator.document.InputValue;
import io.graphoenix.graphql.generator.document.InterfaceType;
import io.graphoenix.graphql.generator.document.ObjectType;
import io.graphoenix.graphql.generator.document.Schema;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.antlr.v4.runtime.RuleContext;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public void buildManager() throws IOException, URISyntaxException {
        graphQLConfigRegister.registerConfig();
        if (graphQLConfig.getBuild()) {
            manager.registerGraphQL(buildDocument().toString());
        }
        mapper.registerFieldMaps();
    }

    public Document buildDocument() throws IOException {
        manager.registerFileByName("graphql/preset.gql");
        Optional<GraphqlParser.ObjectTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeName().flatMap(manager::getObject);
        ObjectType queryType;
        if (queryOperationTypeDefinition.isPresent()) {
            queryType = new ObjectType().setName(queryOperationTypeDefinition.get().name().getText())
                    .setDescription(queryOperationTypeDefinition.get().description() == null ? null : queryOperationTypeDefinition.get().description().getText())
                    .setDirectives(queryOperationTypeDefinition.get().directives() == null ? null : queryOperationTypeDefinition.get().directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()))
                    .setFields(queryOperationTypeDefinition.get().fieldsDefinition() == null ? null : queryOperationTypeDefinition.get().fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, false)).collect(Collectors.toList()));
        } else {
            queryType = new ObjectType().setName("QueryType");
        }
        queryType.addFields(buildQueryTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());

        Optional<GraphqlParser.ObjectTypeDefinitionContext> mutationOperationTypeDefinition = manager.getMutationOperationTypeName().flatMap(manager::getObject);
        ObjectType mutationType;
        if (mutationOperationTypeDefinition.isPresent()) {
            mutationType = new ObjectType().setName(mutationOperationTypeDefinition.get().name().getText())
                    .setDescription(mutationOperationTypeDefinition.get().description() == null ? null : mutationOperationTypeDefinition.get().description().getText())
                    .setDirectives(mutationOperationTypeDefinition.get().directives() == null ? null : mutationOperationTypeDefinition.get().directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()))
                    .setFields(mutationOperationTypeDefinition.get().fieldsDefinition() == null ? null : mutationOperationTypeDefinition.get().fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, true)).collect(Collectors.toList()));
        } else {
            mutationType = new ObjectType().setName("MutationType");
        }
        mutationType.addFields(buildMutationTypeFields()).addInterface(META_INTERFACE_NAME).addFields(getMetaInterfaceFields());

        return new Document()
                .addDefinition(new Schema().setQuery(queryType.getName()).setMutation(mutationType.getName()).toString())
                .addDefinition(queryType.toString())
                .addDefinition(mutationType.toString())
                .addDefinitions(manager.getObjects().map(this::buildObject).map(ObjectType::toString).collect(Collectors.toList()))
                .addDefinitions(manager.getInterfaces().map(this::buildInterface).map(InterfaceType::toString).collect(Collectors.toList()))
                .addDefinitions(manager.getEnums().map(this::buildEnum).map(EnumType::toString).collect(Collectors.toList()))
                .addDefinitions(buildObjectExpressions().stream().map(InputObjectType::toString).collect(Collectors.toList()))
                .addDefinitions(manager.getDirectives().map(this::buildDirective).map(Directive::toString).collect(Collectors.toList()));
    }

    public Document getDocument() {
        return new Document()
                .addDefinition(manager.getSchema().getText())
                .addDefinitions(manager.getScalars().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getEnums().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getInterfaces().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getObjects().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getInputObjects().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getUnions().map(RuleContext::getText).collect(Collectors.toList()))
                .addDefinitions(manager.getDirectives().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public ObjectType buildObject(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        return new ObjectType()
                .setName(objectTypeDefinitionContext.name().getText())
                .setDescription(objectTypeDefinitionContext.description() == null ? null : objectTypeDefinitionContext.description().getText())
                .setInterfaces(objectTypeDefinitionContext.implementsInterfaces() == null ? null : objectTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toList()))
                .addInterface(META_INTERFACE_NAME)
                .setFields(objectTypeDefinitionContext.fieldsDefinition() == null ? null : objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))).collect(Collectors.toList()))
                .addFields(getMetaInterfaceFields())
                .setDirectives(objectTypeDefinitionContext.directives() == null ? null : objectTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public InterfaceType buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {

        return new InterfaceType()
                .setName(interfaceTypeDefinitionContext.name().getText())
                .setDescription(interfaceTypeDefinitionContext.description() == null ? null : interfaceTypeDefinitionContext.description().getText())
                .setInterfaces(interfaceTypeDefinitionContext.implementsInterfaces() == null ? null : interfaceTypeDefinitionContext.implementsInterfaces().typeName().stream().map(typeNameContext -> typeNameContext.name().getText()).collect(Collectors.toList()))
                .setFields(interfaceTypeDefinitionContext.fieldsDefinition() == null ? null : interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, false)).collect(Collectors.toList()))
                .setDirectives(interfaceTypeDefinitionContext.directives() == null ? null : interfaceTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public List<Field> getMetaInterfaceFields() {
        return manager.getInterface(META_INTERFACE_NAME).orElseThrow()
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildFiled(fieldDefinitionContext, false)).collect(Collectors.toList());
    }

    public EnumType buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return new EnumType()
                .setName(enumTypeDefinitionContext.name().getText())
                .setDescription(enumTypeDefinitionContext.description() == null ? null : enumTypeDefinitionContext.description().getText())
                .setEnumValues(enumTypeDefinitionContext.enumValueDefinitions() == null ? null : enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream().map(this::buildEnumValue).collect(Collectors.toList()))
                .setDirectives(enumTypeDefinitionContext.directives() == null ? null : enumTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public Field buildFiled(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean isMutationOperationType) {
        Field field = new Field().setName(fieldDefinitionContext.name().getText())
                .setDescription(fieldDefinitionContext.description() == null ? null : fieldDefinitionContext.description().getText())
                .setTypeName(fieldDefinitionContext.type().getText())
                .setArguments(fieldDefinitionContext.argumentsDefinition() == null ? null : fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toList()))
                .setDirectives(fieldDefinitionContext.directives() == null ? null : fieldDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> filedObjectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()));
        if (filedObjectTypeDefinitionContext.isPresent()) {
            if (isMutationOperationType) {
                field.addArguments(buildArgumentsFromObjectType(filedObjectTypeDefinitionContext.get(), InputType.INPUT));
            } else {
                field.addArguments(buildArgumentsFromObjectType(filedObjectTypeDefinitionContext.get(), InputType.EXPRESSION));
            }
        } else if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            field.addArgument(filedToArgument(fieldDefinitionContext, InputType.EXPRESSION));
        }
        return field;
    }

    public EnumValue buildEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        return new EnumValue().setName(enumValueDefinitionContext.enumValue().enumValueName().getText())
                .setDescription(enumValueDefinitionContext.description() == null ? null : enumValueDefinitionContext.description().getText())
                .setDirectives(enumValueDefinitionContext.directives() == null ? null : enumValueDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public InputValue buildInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new InputValue().setName(inputValueDefinitionContext.name().getText())
                .setDefaultValue(inputValueDefinitionContext.defaultValue() == null ? null : inputValueDefinitionContext.defaultValue().value().getText())
                .setDescription(inputValueDefinitionContext.description() == null ? null : inputValueDefinitionContext.description().getText())
                .setTypeName(inputValueDefinitionContext.type().getText())
                .setDirectives(inputValueDefinitionContext.directives() == null ? null : inputValueDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toList()));
    }

    public Directive buildDirective(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        return new Directive().setName(directiveDefinitionContext.name().getText())
                .setDescription(directiveDefinitionContext.description() == null ? null : directiveDefinitionContext.description().getText())
                .setArguments(directiveDefinitionContext.argumentsDefinition() == null ? null : directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(this::buildInputValue).collect(Collectors.toList()))
                .setDirectiveLocations(directiveDefinitionContext.directiveLocations() == null ? null : directiveLocationList(directiveDefinitionContext.directiveLocations()));
    }

    public List<String> directiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        List<String> directiveLocationList = new ArrayList<>();
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

    public List<InputValue> buildArgumentsFromObjectType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, InputType inputType) {
        List<InputValue> inputValueList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> filedToArgument(fieldDefinitionContext, inputType))
                .collect(Collectors.toList());
        manager.getInterface(META_INTERFACE_NAME)
                .ifPresent(interfaceTypeDefinitionContext -> {
                    interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                            .forEach(fieldDefinitionContext ->
                                    inputValueList.add(filedToArgument(fieldDefinitionContext, inputType))
                            );
                });
        return inputValueList;
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
