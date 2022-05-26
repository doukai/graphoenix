package io.graphoenix.json.schema.translator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;

@ApplicationScoped
public class JsonSchemaTranslator {

    private final GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(GraphQLConfig graphQLConfig,
                                IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }


    protected JsonElement objectToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("type", "object");
        jsonSchema.add("properties", objectToProperties(objectTypeDefinitionContext));

        return jsonSchema;
    }

    protected JsonElement objectToProperties(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        JsonObject properties = new JsonObject();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            properties.add(fieldDefinitionContext.name().getText(), fieldToProperty(fieldDefinitionContext.type()));
                        }
                );
        return properties;
    }

    protected JsonElement fieldToProperty(GraphqlParser.TypeContext typeContext) {
        JsonObject property = new JsonObject();
        if (manager.fieldTypeIsList(typeContext)) {
            property.addProperty("type", "array");
            property.add("items", fieldToProperty(typeContext.listType().type()));

        } else {
            String fieldTypeName = manager.getFieldTypeName(typeContext);
            switch (fieldTypeName) {
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    property.addProperty("type", "string");
                case "Boolean":
                    property.addProperty("type", "boolean");
                case "Int":
                case "BigInteger":
                case "Float":
                case "BigDecimal":
                    property.addProperty("type", "number");
            }
            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext));
        }
        return property;
    }
}
