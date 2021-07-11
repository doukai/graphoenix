package graphql.parser;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.statement.SetStatement;
import org.antlr.v4.runtime.tree.TerminalNode;

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
        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            typeDefinitionContextMap.put(typeDefinitionContext.inputObjectTypeDefinition().name().getText(), typeDefinitionContext);
            Map<String, String> fieldTypeNameMap = new HashMap<>();
            typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition()
                    .forEach(inputValueDefinitionContext -> fieldTypeNameMap.put(inputValueDefinitionContext.name().getText(),
                            getFieldTypeName(inputValueDefinitionContext.type())));
            typeFieldTypeNameMap.put(typeDefinitionContext.inputObjectTypeDefinition().name().getText(), fieldTypeNameMap);
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

    public boolean isInputObject(String name) {
        return typeDefinitionContextMap.keySet().stream().anyMatch(key -> key.equals(name) && typeDefinitionContextMap.get(key).inputObjectTypeDefinition() != null);
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

        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            return typeDefinitionContext.inputObjectTypeDefinition().INPUT().getText();
        }

        return null;
    }

    public GraphqlParser.TypeDefinitionContext getDefinition(String name) {
        return typeDefinitionContextMap.get(name);
    }


    public String getQuerySchemaFieldTypeName(String querySchemaFieldName) {

        return typeFieldTypeNameMap.get(getQueryTypeName()).get(querySchemaFieldName);
    }

    public String getObjectFieldTypeName(String objectName, String filedName) {

        return typeFieldTypeNameMap.get(objectName).get(filedName);
    }

    public Optional<GraphqlParser.TypeContext> getObjectFieldTypeContext(String objectName, String filedName) {

        return typeDefinitionContextMap.get(objectName)
                .objectTypeDefinition().fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(filedName)).findFirst().map(GraphqlParser.FieldDefinitionContext::type);
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getObjectFieldDefinitionContext(String objectName, String filedName) {

        return typeDefinitionContextMap.get(objectName)
                .objectTypeDefinition().fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(filedName)).findFirst();
    }


    public Optional<GraphqlParser.TypeContext> getQueryObjectFieldType(String filedName) {

        return typeDefinitionContextMap.get(getQueryTypeName())
                .objectTypeDefinition().fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(filedName)).findFirst().map(GraphqlParser.FieldDefinitionContext::type);
    }

    public String getTypeIdFieldName(String typeName) {

        long IdFieldCount = typeFieldTypeNameMap.get(typeName).entrySet().stream().filter(entry -> entry.getValue().equals("ID")).count();
        if (IdFieldCount == 1) {

            return typeFieldTypeNameMap.get(typeName).entrySet().stream().filter(entry -> entry.getValue().equals("ID")).map(Map.Entry::getKey).findFirst().orElse(null);
        }

        return null;
    }

    public String getTypeRelationFieldName(String sourceName, String targetName) {

        long IdFieldCount = typeFieldTypeNameMap.get(sourceName).entrySet().stream().filter(entry -> entry.getValue().equals(targetName)).count();
        if (IdFieldCount == 1) {

            return typeFieldTypeNameMap.get(sourceName).entrySet().stream().filter(entry -> entry.getValue().equals(targetName)).map(Map.Entry::getKey).findFirst().orElse(null);
        }

        return null;
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
            } else return typeContext.nonNullType().listType() != null;
        } else return typeContext.listType() != null;
    }

    public String getQueryTypeName() {
        return schemaDefinitionContextMap.entrySet().stream()
                .filter(stringStringEntry -> stringStringEntry.getValue().equals("query")).findFirst().map(Map.Entry::getKey).orElse(null);
    }

    public String getMutationTypeName() {
        return schemaDefinitionContextMap.entrySet().stream()
                .filter(stringStringEntry -> stringStringEntry.getValue().equals("mutation")).findFirst().map(Map.Entry::getKey).orElse(null);
    }

    public boolean isFieldOfType(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return typeDefinitionContextMap.get(getFieldTypeName(fieldTypeContext)).objectTypeDefinition().fieldsDefinition().fieldDefinition().stream().anyMatch(fieldDefinitionContext ->
                fieldDefinitionContext.name().getText().equals(inputValueDefinitionContext.name().getText()));
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromArgumentsDefinitionContext(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldWithVariableContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitionFromInputObjectTypeDefinitionContext(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ArgumentContext> getArgumentFromInputValueDefinition(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ObjectFieldWithVariableContext> getObjectFieldWithVariableFromInputValueDefinition(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public Optional<GraphqlParser.ObjectFieldContext> getObjectFieldFromInputValueDefinition(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return objectValueContext.objectField().stream().filter(objectFieldContext -> objectFieldContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    protected Expression scalarValueToDBValue(GraphqlParser.ValueContext valueContext) {
        return scalarValueToDBValue(valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    public Expression scalarValueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToDBValue(valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    public Expression scalarValueToDBValue(TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            return new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText()));
        } else if (intValue != null) {
            return new LongValue(intValue.getText());
        } else if (floatValue != null) {
            return new DoubleValue(floatValue.getText());
        } else if (booleanValue != null) {
            //todo
        } else if (nullValue != null) {
            return new NullValue();
        }
        return null;
    }

    public Optional<GraphqlParser.FieldDefinitionContext> getTypeFieldDefinitionFromInputValueDefinition(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = getFieldTypeDefinition(typeContext).objectTypeDefinition();
        return objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext ->
                fieldDefinitionContext.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
    }

    public GraphqlParser.TypeDefinitionContext getFieldTypeDefinition(GraphqlParser.TypeContext typeContext) {
        return getDefinition(getFieldTypeName(typeContext));
    }

    public GraphqlParser.TypeDefinitionContext getFieldTypeDefinition(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return getDefinition(getFieldTypeName(inputValueDefinitionContext.type()));
    }

    protected Optional<GraphqlParser.ArgumentContext> getIdArgument(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        String typeIdFieldName = getTypeIdFieldName(getFieldTypeName(typeContext));
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Optional<GraphqlParser.ObjectFieldWithVariableContext> getIdObjectFieldWithVariable(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String typeIdFieldName = getTypeIdFieldName(getFieldTypeName(typeContext));
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(fieldWithVariableContext -> fieldWithVariableContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Optional<GraphqlParser.ObjectFieldContext> getIdObjectField(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        String typeIdFieldName = getTypeIdFieldName(getFieldTypeName(typeContext));
        return objectValueContext.objectField().stream().filter(fieldContext -> fieldContext.name().getText().equals(typeIdFieldName)).findFirst();
    }
    protected String getIdVariableName(GraphqlParser.TypeContext typeContext) {
        String typeName = getFieldTypeName(typeContext);
        return DBNameConverter.INSTANCE.graphqlFieldNameToVariableName(typeName, getTypeIdFieldName(typeName));
    }

    protected SetStatement createInsertIdSetStatement(GraphqlParser.TypeContext typeContext) {
        String idVariableName = "@" + getIdVariableName(typeContext);
        Function function = new Function();
        function.setName("LAST_INSERT_ID");
        return new SetStatement(idVariableName, function);
    }

    protected UserVariable createInsertIdUserVariable(GraphqlParser.TypeContext typeContext) {
        String idVariableName = getIdVariableName(typeContext);
        UserVariable userVariable = new UserVariable();
        userVariable.setName(idVariableName);
        return userVariable;
    }
}
