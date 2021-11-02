package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;

public class GraphqlTypeToTable {

    final private IGraphqlDocumentManager manager;

    public GraphqlTypeToTable(IGraphqlDocumentManager graphqlAntlrManager) {
        this.manager = graphqlAntlrManager;
    }


    public List<String> createTablesSql() {
        return manager.getObjects().map(this::createTable).map(CreateTable::toString).collect(Collectors.toList());
    }

    public List<String> createTablesSql(String graphql) {
        return createTablesSql(DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    public List<String> createTablesSql(IGraphqlDocumentManager manager) {
        return manager.getObjects().map(this::createTable).map(CreateTable::toString).collect(Collectors.toList());
    }

    public List<String> createTablesSql(GraphqlParser.DocumentContext documentContext) {
        return createTables(documentContext).stream()
                .map(CreateTable::toString).collect(Collectors.toList());
    }

    public List<CreateTable> createTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createTable).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    protected Optional<CreateTable> createTable(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() == null) {
            return Optional.empty();
        }
        return createTable(definitionContext.typeSystemDefinition());
    }

    protected Optional<CreateTable> createTable(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.typeDefinition() == null) {
            return Optional.empty();
        }
        return createTable(typeSystemDefinitionContext.typeDefinition());
    }

    protected Optional<CreateTable> createTable(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.objectTypeDefinition() == null) {
            return Optional.empty();
        }
        if (manager.isOperation(typeDefinitionContext.objectTypeDefinition().name().getText())) {
            return Optional.empty();
        }
        return Optional.of(createTable(typeDefinitionContext.objectTypeDefinition()));
    }

    protected CreateTable createTable(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        CreateTable createTable = new CreateTable();
        Table table = DB_NAME_UTIL.typeToTable(objectTypeDefinitionContext);
        createTable.setTable(table);
        createTable.setColumnDefinitions(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(this::createColumn).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        createTable.setIfNotExists(true);
        createTable.setTableOptionsStrings(createTableOption(objectTypeDefinitionContext));
        return createTable;
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.type().typeName() != null) {
            return createColumn(fieldDefinitionContext, fieldDefinitionContext.type().typeName(), false);
        } else if (fieldDefinitionContext.type().listType() != null) {
            return Optional.empty();
        } else if (fieldDefinitionContext.type().nonNullType() != null) {
            if (fieldDefinitionContext.type().nonNullType().typeName() != null) {
                return createColumn(fieldDefinitionContext, fieldDefinitionContext.type().nonNullType().typeName(), true);
            } else if (fieldDefinitionContext.type().nonNullType().listType() != null) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.TypeNameContext typeNameContext, boolean nonNull) {
        if (manager.isObject(typeNameContext.name().getText())) {
            return Optional.empty();
        }
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
        columnDefinition.setColDataType(createColDataType(typeNameContext, fieldDefinitionContext.directives()));
        List<String> columnSpecs = new ArrayList<>();
        if (typeNameContext.name().getText().equals("ID")) {
            columnSpecs.add("PRIMARY KEY");
        }
        if (nonNull) {
            columnSpecs.add("NOT NULL");
        }
        columnDirectiveToColumnSpecs(fieldDefinitionContext.directives()).ifPresent(columnSpecs::addAll);
        if (fieldDefinitionContext.description() != null) {
            columnSpecs.add("COMMENT " + DB_NAME_UTIL.graphqlDescriptionToDBComment(fieldDefinitionContext.description().getText()));
        }
        columnDefinition.setColumnSpecs(columnSpecs);
        return Optional.of(columnDefinition);
    }

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext) {
        if (manager.isEnum(typeNameContext.name().getText())) {
            return createEnumColDataType(typeNameContext);
        } else if (manager.isScaLar(typeNameContext.name().getText())) {
            return createScalarColDataType(typeNameContext, directivesContext);
        }
        return null;
    }

    protected ColDataType createEnumColDataType(GraphqlParser.TypeNameContext typeNameContext) {
        ColDataType colDataType = new ColDataType();
        colDataType.setDataType("ENUM");

        colDataType.setArgumentsStringList(manager.getEnum(typeNameContext.name().getText()).map(enumTypeDefinitionContext -> enumTypeDefinitionContext.enumValueDefinitions()
                .enumValueDefinition().stream()
                .map(value -> DB_NAME_UTIL.stringValueToDBVarchar(value.getText())).collect(Collectors.toList())).orElse(Collections.emptyList()));

        return colDataType;
    }

    protected ColDataType createScalarColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> dataTypeDirective = getDataTypeDirective(directivesContext);
        Optional<GraphqlParser.ArgumentContext> dataType = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("type")).findFirst());
        Optional<GraphqlParser.ArgumentContext> length = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("length")).findFirst());
        Optional<GraphqlParser.ArgumentContext> decimals = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("decimals")).findFirst());

        List<String> argumentsStringList = new ArrayList<>();
        ColDataType colDataType = new ColDataType();
        switch (typeNameContext.name().getText()) {
            case "ID":
            case "Int":
                colDataType.setDataType(dataType.map(argumentContext -> DB_NAME_UTIL.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("INT"));
                argumentsStringList.add(length.map(argumentContext -> argumentContext.valueWithVariable().IntValue().getText()).orElse("255"));
                break;
            case "Boolean":
                colDataType.setDataType(dataType.map(argumentContext -> DB_NAME_UTIL.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("BOOL"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "String":
                colDataType.setDataType(dataType.map(argumentContext -> DB_NAME_UTIL.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("VARCHAR"));
                argumentsStringList.add(length.map(argumentContext -> argumentContext.valueWithVariable().IntValue().getText()).orElse("255"));
                break;
            case "Float":
                colDataType.setDataType(dataType.map(argumentContext -> DB_NAME_UTIL.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("FLOAT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
        }

        if (!argumentsStringList.isEmpty()) {
            colDataType.setArgumentsStringList(argumentsStringList);
        }
        return colDataType;
    }

    protected List<String> createTableOption(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<String> tableOptionsList = new ArrayList<>();
        if (objectTypeDefinitionContext.directives() != null) {
            directiveToTableOption(objectTypeDefinitionContext.directives()).ifPresent(tableOptionsList::addAll);
        }
        if (objectTypeDefinitionContext.description() != null) {
            tableOptionsList.add("COMMENT " + DB_NAME_UTIL.graphqlDescriptionToDBComment(objectTypeDefinitionContext.description().getText()));
        }
        return tableOptionsList;
    }

    protected Optional<List<String>> directiveToTableOption(GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> tableDirective = getTableDirective(directivesContext);
        return tableDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToTableOption).collect(Collectors.toList()));
    }

    protected String argumentToTableOption(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().IntValue() != null) {
            return DB_NAME_UTIL.directiveToTableOption(argumentContext.name().getText(), argumentContext.valueWithVariable().IntValue().getText());
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return argumentContext.name().getText();
        } else if (argumentContext.valueWithVariable().StringValue() != null) {
            return DB_NAME_UTIL.directiveToTableOption(argumentContext.name().getText(), argumentContext.valueWithVariable().StringValue().getText());
        }
        //TODO
        return null;
    }

    protected Optional<List<String>> columnDirectiveToColumnSpecs(GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> columnDirective = getColumnDirective(directivesContext);
        return columnDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToColumnSpecs).collect(Collectors.toList()));
    }

    protected String argumentToColumnSpecs(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().IntValue() != null) {
            return DB_NAME_UTIL.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().IntValue().getText());
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return DB_NAME_UTIL.booleanDirectiveTocColumnDefinition(argumentContext.name().getText());
        } else if (argumentContext.valueWithVariable().StringValue() != null) {
            return DB_NAME_UTIL.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().StringValue().getText());
        }
        //TODO
        return null;
    }

    protected Optional<GraphqlParser.DirectiveContext> getTableDirective(GraphqlParser.DirectivesContext directivesContext) {
        if (directivesContext == null) {
            return Optional.empty();
        }
        return directivesContext.directive().stream().filter(directiveContext -> directiveContext.name().getText().equals("table")).findFirst();
    }

    protected Optional<GraphqlParser.DirectiveContext> getColumnDirective(GraphqlParser.DirectivesContext directivesContext) {
        if (directivesContext == null) {
            return Optional.empty();
        }
        return directivesContext.directive().stream().filter(directiveContext -> directiveContext.name().getText().equals("column")).findFirst();
    }

    protected Optional<GraphqlParser.DirectiveContext> getDataTypeDirective(GraphqlParser.DirectivesContext directivesContext) {
        if (directivesContext == null) {
            return Optional.empty();
        }
        return directivesContext.directive().stream().filter(directiveContext -> directiveContext.name().getText().equals("dataType")).findFirst();
    }
}
