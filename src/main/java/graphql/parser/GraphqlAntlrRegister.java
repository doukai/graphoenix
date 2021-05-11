package graphql.parser;

import graphql.parser.antlr.GraphqlParser;

import java.util.HashMap;
import java.util.Map;

public class GraphqlAntlrRegister {

    private final Map<String, String> schemaDefinitionContextMap = new HashMap<>();

    private final Map<String, GraphqlParser.TypeDefinitionContext> typeDefinitionContextMap = new HashMap<>();

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
}
