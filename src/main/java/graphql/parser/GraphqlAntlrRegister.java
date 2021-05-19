package graphql.parser;

import graphql.parser.antlr.GraphqlParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GraphqlAntlrRegister {

    private final Map<String, String> schemaDefinitionContextMap = new HashMap<>();

    private final Map<String, GraphqlParser.TypeDefinitionContext> typeDefinitionContextMap = new HashMap<>();

    private final Map<String, Map<String, String>> typeFieldTypeNameMap = new HashMap<>();

    private final String[] innerScalarType = {"ID", "Boolean", "String", "Float", "Int"};

    public void registerDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::registerDefinition);
    }

    protected void registerDefinition(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() != null) {
            registerSystemDefinition(definitionContext.typeSystemDefinition());
        }
    }

    protected void registerSystemDefinition(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.schemaDefinition() != null) {
            typeSystemDefinitionContext.schemaDefinition().operationTypeDefinition().forEach(this::registerOperationType);
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            registerTypeDefinition(typeSystemDefinitionContext.typeDefinition());
        }
    }

    protected void registerOperationType(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        schemaDefinitionContextMap.put(operationTypeDefinitionContext.typeName().name().getText(), operationTypeDefinitionContext.operationType().getText());
    }

    protected void registerTypeDefinition(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.enumTypeDefinition() != null) {
            typeDefinitionContextMap.put(typeDefinitionContext.enumTypeDefinition().name().getText(), typeDefinitionContext);
        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            typeDefinitionContextMap.put(typeDefinitionContext.objectTypeDefinition().name().getText(), typeDefinitionContext);
            Map<String, String> fieldTypeNameMap = new HashMap<>();
            typeDefinitionContext.objectTypeDefinition().fieldsDefinition().fieldDefinition()
                    .forEach(fieldDefinitionContext -> fieldTypeNameMap.put(fieldDefinitionContext.name().getText(),
                            getFieldTypeName(fieldDefinitionContext.type())));
            typeFieldTypeNameMap.put(typeDefinitionContext.objectTypeDefinition().name().getText(), fieldTypeNameMap);
        } else if (typeDefinitionContext.scalarTypeDefinition() != null) {
            typeDefinitionContextMap.put(typeDefinitionContext.scalarTypeDefinition().name().getText(), typeDefinitionContext);
        }
    }

    public boolean exist(String name) {
        return typeDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name));
    }

    public boolean isEnum(String name) {
        return typeDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name) && typeDefinitionContextMap.get(key).enumTypeDefinition() != null);
    }

    public boolean isObject(String name) {
        return typeDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name) && typeDefinitionContextMap.get(key).objectTypeDefinition() != null);
    }

    public boolean isScaLar(String name) {
        return typeDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name) && typeDefinitionContextMap.get(key).scalarTypeDefinition() != null);
    }

    protected boolean isInnerScalar(String name) {
        return Arrays.asList(innerScalarType).contains(name);
    }

    public boolean inSchema(String name) {
        return schemaDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name));
    }

    public String getDefinitionType(String name) {
        final GraphqlParser.TypeDefinitionContext typeDefinitionContext = typeDefinitionContextMap.get(name);

        if (typeDefinitionContext.enumTypeDefinition() != null) {
            return typeDefinitionContext.enumTypeDefinition().ENUM().getText();

        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            return typeDefinitionContext.objectTypeDefinition().TYPE().getText();

        } else if (typeDefinitionContext.scalarTypeDefinition() != null) {
            return typeDefinitionContext.scalarTypeDefinition().SCALAR().getText();
        }

        return null;
    }

    public GraphqlParser.TypeDefinitionContext getDefinition(String name) {
        return typeDefinitionContextMap.get(name);
    }


    public String getQuerySchemaFieldTypeName(String querySchemaFieldName) {

        return typeFieldTypeNameMap.get(getQueryTypeName()).get(querySchemaFieldName);
    }


    public Optional<GraphqlParser.TypeContext> getObjectFieldType(String objectName, String filedName) {

        String filedTypeName = typeFieldTypeNameMap.get(objectName).get(filedName);

        return typeDefinitionContextMap.get(filedTypeName)
                .objectTypeDefinition().fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(filedName)).findFirst().map(GraphqlParser.FieldDefinitionContext::type);
    }


    public Optional<GraphqlParser.TypeContext> getQueryObjectFieldType(String filedName) {

        String filedTypeName = typeFieldTypeNameMap.get(getQueryTypeName()).get(filedName);

        return typeDefinitionContextMap.get(filedTypeName)
                .objectTypeDefinition().fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(filedName)).findFirst().map(GraphqlParser.FieldDefinitionContext::type);
    }

    public String getFieldTypeName(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return typeContext.typeName().name().getText();
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return typeContext.nonNullType().typeName().name().getText();
            } else if (typeContext.nonNullType().listType() != null) {
                return getFieldTypeName(typeContext.nonNullType().listType().type());
            }
        } else if (typeContext.listType() != null) {
            return getFieldTypeName(typeContext.listType().type());
        }

        return null;
    }

    public boolean fieldTypeIsList(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return false;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return false;
            } else if (typeContext.nonNullType().listType() != null) {
                return true;
            }
        } else if (typeContext.listType() != null) {
            return true;
        }

        return false;
    }

    public String getQueryTypeName() {
        return schemaDefinitionContextMap.entrySet().stream()
                .filter(stringStringEntry -> stringStringEntry.getValue().equals("query")).findFirst().map(Map.Entry::getKey).orElse(null);
    }
}
