package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class GraphqlAntlrToTable {

    final private GraphqlAntlrRegister graphqlAntlrRegister;

    private final String[] scalarType = {"ID", "Boolean", "String", "Float", "Int"};

    public GraphqlAntlrToTable(GraphqlAntlrRegister graphqlAntlrRegister) {
        this.graphqlAntlrRegister = graphqlAntlrRegister;
    }

    abstract List<CreateTable> createTables(GraphqlParser.DocumentContext documentContext);

    protected boolean isScalar(String name) {

        return Arrays.asList(scalarType).contains(name);
    }

    protected boolean isEnum(String name) {

        return graphqlAntlrRegister.isEnum(name);
    }

    protected boolean isObject(String name) {

        return graphqlAntlrRegister.isObject(name);
    }

    protected boolean inSchema(String name) {

        return graphqlAntlrRegister.inSchema(name);
    }

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext, boolean list) {

        if (graphqlAntlrRegister.exist(typeNameContext.name().getText())) {
            String definitionType = graphqlAntlrRegister.getDefinitionType(typeNameContext.name().getText()).toLowerCase();
            if (definitionType.equals("type")) {
                return createTypeColDataType(typeNameContext, directivesContext);
            } else if (definitionType.equals("enum")) {
                return createEnumColDataType(typeNameContext, list);
            }
        } else if (isScalar(typeNameContext.name().getText())) {

            return createDefaultScalarColDataType(typeNameContext, directivesContext);
        }
        //TODO
        return null;
    }

    protected ColDataType createTypeColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> columnDirective = getColumnDirective(directivesContext);
        Optional<GraphqlParser.ArgumentContext> dataType = columnDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("type")).findFirst());
        Optional<GraphqlParser.ArgumentContext> length = columnDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("length")).findFirst());

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        colDataType.setDataType(dataType.map(argumentContext -> DBNameConverter.INSTANCE.graphqlTypeToDBType(argumentContext.valueWithVariable().StringValue().getText())).orElse("INT"));
        length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
        colDataType.setArgumentsStringList(argumentsStringList);

        return colDataType;
    }

    protected ColDataType createEnumColDataType(GraphqlParser.TypeNameContext typeNameContext, boolean list) {
        ColDataType colDataType = new ColDataType();
        if (list) {
            colDataType.setDataType("SET");
        } else {
            colDataType.setDataType("ENUM");
        }
        colDataType.setArgumentsStringList(graphqlAntlrRegister.getDefinition(typeNameContext.name().getText())
                .enumTypeDefinition()
                .enumValueDefinitions()
                .enumValueDefinition().stream()
                .map(value -> DBNameConverter.INSTANCE.stringValueToDBVarchar(value.getText()))
                .collect(Collectors.toList()));

        return colDataType;
    }

    protected ColDataType createDefaultScalarColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> columnDirective = getColumnDirective(directivesContext);
        Optional<GraphqlParser.ArgumentContext> dataType = columnDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("type")).findFirst());
        Optional<GraphqlParser.ArgumentContext> length = columnDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("length")).findFirst());
        Optional<GraphqlParser.ArgumentContext> decimals = columnDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("decimals")).findFirst());
        List<String> argumentsStringList = new ArrayList<>();
        ColDataType colDataType = new ColDataType();
        switch (typeNameContext.name().getText()) {
            case "ID":
            case "Int":
                colDataType.setDataType(dataType.map(argumentContext -> DBNameConverter.INSTANCE.graphqlTypeToDBType(argumentContext.valueWithVariable().StringValue().getText())).orElse("INT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Boolean":
                colDataType.setDataType(dataType.map(argumentContext -> DBNameConverter.INSTANCE.graphqlTypeToDBType(argumentContext.valueWithVariable().StringValue().getText())).orElse("BOOL"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "String":
                colDataType.setDataType(dataType.map(argumentContext -> DBNameConverter.INSTANCE.graphqlTypeToDBType(argumentContext.valueWithVariable().StringValue().getText())).orElse("VARCHAR"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Float":
                colDataType.setDataType(dataType.map(argumentContext -> DBNameConverter.INSTANCE.graphqlTypeToDBType(argumentContext.valueWithVariable().StringValue().getText())).orElse("FLOAT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
        }

        if (!argumentsStringList.isEmpty()) {
            colDataType.setArgumentsStringList(argumentsStringList);
        }
        return colDataType;
    }

    protected Optional<GraphqlParser.DirectiveContext> getTableDirective(GraphqlParser.DirectivesContext directivesContext) {
        if(directivesContext == null){
            return Optional.empty();
        }
        return directivesContext.directive().stream().filter(directiveContext -> directiveContext.name().getText().equals("@table")).findFirst();
    }

    protected Optional<GraphqlParser.DirectiveContext> getColumnDirective(GraphqlParser.DirectivesContext directivesContext) {
        if(directivesContext == null){
            return Optional.empty();
        }
        return directivesContext.directive().stream().filter(directiveContext -> directiveContext.name().getText().equals("@column")).findFirst();
    }

    protected List<String> createTableOption(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<String> tableOptionsList = new ArrayList<>();
        if (objectTypeDefinitionContext.directives() != null) {

            directiveToTableOption(objectTypeDefinitionContext.directives()).ifPresent(tableOptionsList::addAll);
        }
        if (objectTypeDefinitionContext.description() != null) {
            tableOptionsList.add("COMMENT " + DBNameConverter.INSTANCE.graphqlDescriptionToDBComment(objectTypeDefinitionContext.description().getText()));
        }
        return tableOptionsList;
    }

    protected List<String> createRelationTableOption(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        List<String> tableOptionsList = new ArrayList<>();
        if (objectTypeDefinitionContext.directives() != null) {

            directiveToTableOption(objectTypeDefinitionContext.directives()).ifPresent(tableOptionsList::addAll);
        }
        if (fieldDefinitionContext.description() != null) {
            tableOptionsList.add("COMMENT " + DBNameConverter.INSTANCE.graphqlDescriptionToDBComment(fieldDefinitionContext.description().getText()));
        }
        return tableOptionsList;
    }

    protected Optional<List<String>> directiveToTableOption(GraphqlParser.DirectivesContext directivesContext) {

        Optional<GraphqlParser.DirectiveContext> tableDirective = getTableDirective(directivesContext);
        return tableDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToTableOption).collect(Collectors.toList()));
    }

    protected String argumentToTableOption(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().IntValue() != null) {
            return DBNameConverter.INSTANCE.directiveToTableOption(argumentContext.name().getText(), argumentContext.valueWithVariable().IntValue().getText());
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return argumentContext.name().getText();
        } else if (argumentContext.valueWithVariable().StringValue() != null) {
            return DBNameConverter.INSTANCE.directiveToTableOption(argumentContext.name().getText(), argumentContext.valueWithVariable().StringValue().getText());
        }
        //TODO
        return null;
    }

    protected List<String> createColumnSpecs(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        List<String> columnSpecsList = new ArrayList<>();
        if (fieldDefinitionContext.directives() != null) {

            directiveToColumnSpecs(fieldDefinitionContext.directives()).ifPresent(columnSpecsList::addAll);
        }
        if (fieldDefinitionContext.description() != null) {
            columnSpecsList.add("COMMENT " + DBNameConverter.INSTANCE.graphqlDescriptionToDBComment(fieldDefinitionContext.description().getText()));
        }
        return columnSpecsList;
    }

    protected Optional<List<String>> directiveToColumnSpecs(GraphqlParser.DirectivesContext directivesContext) {

        Optional<GraphqlParser.DirectiveContext> columnDirective = getColumnDirective(directivesContext);
        return columnDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToColumnSpecs).collect(Collectors.toList()));
    }

    protected String argumentToColumnSpecs(GraphqlParser.ArgumentContext argumentContext) {

        if (argumentContext.valueWithVariable().IntValue() != null) {
            return DBNameConverter.INSTANCE.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().IntValue().getText());
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return argumentContext.name().getText();
        } else if (argumentContext.valueWithVariable().StringValue() != null) {
            return DBNameConverter.INSTANCE.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().StringValue().getText());
        }
        //TODO
        return null;
    }
}
