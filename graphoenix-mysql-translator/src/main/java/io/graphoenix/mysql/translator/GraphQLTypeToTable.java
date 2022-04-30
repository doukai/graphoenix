package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.mysql.utils.DBNameUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.DEFINITION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_DEFINITION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;

@ApplicationScoped
public class GraphQLTypeToTable {

    private final IGraphQLDocumentManager manager;
    private final DBNameUtil dbNameUtil;

    @Inject
    public GraphQLTypeToTable(IGraphQLDocumentManager manager, DBNameUtil dbNameUtil) {
        this.manager = manager;
        this.dbNameUtil = dbNameUtil;
    }

    public Stream<String> createTablesSQL() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::createTable)
                .map(CreateTable::toString);
    }

    public Stream<String> createTablesSQL(IGraphQLDocumentManager manager) {
        return manager.getObjects().map(this::createTable).map(CreateTable::toString);
    }

    public Stream<String> createTablesSQL(GraphqlParser.DocumentContext documentContext) {
        return createTables(documentContext).map(CreateTable::toString);
    }

    public Stream<CreateTable> createTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createTable).filter(Optional::isPresent).map(Optional::get);
    }

    protected Optional<CreateTable> createTable(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() == null) {
            throw new GraphQLErrors(DEFINITION_NOT_EXIST);
        }
        return createTable(definitionContext.typeSystemDefinition());
    }

    protected Optional<CreateTable> createTable(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.typeDefinition() == null) {
            throw new GraphQLErrors(TYPE_DEFINITION_NOT_EXIST);
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

        Table table = new Table(dbNameUtil.graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
        createTable.setTable(table);
        createTable.setColumnDefinitions(
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext ->
                                manager.isNotInvokeField(
                                        objectTypeDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        )
                        .filter(fieldDefinitionContext ->
                                manager.isNotFunctionField(
                                        objectTypeDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        )
                        .filter(fieldDefinitionContext ->
                                manager.isNotConnectionField(
                                        objectTypeDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        )
                        .map(this::createColumn)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
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
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldDefinitionContext.type().getText()));
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.TypeNameContext typeNameContext, boolean nonNull) {
        if (manager.isObject(typeNameContext.name().getText())) {
            return Optional.empty();
        }
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(dbNameUtil.graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
        columnDefinition.setColDataType(createColDataType(typeNameContext, fieldDefinitionContext.directives()));
        List<String> columnSpecs = new ArrayList<>();
        if (typeNameContext.name().getText().equals("ID")) {
            columnSpecs.add("PRIMARY KEY");
        }
        if (nonNull) {
            columnSpecs.add("NOT NULL");
        }
        getDataTypeDirective(fieldDefinitionContext.directives())
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("default")).findFirst())
                .map(argumentContext -> dbNameUtil.directiveToColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().getText()))
                .ifPresent(columnSpecs::add);
        columnDirectiveToColumnSpecs(fieldDefinitionContext.directives()).ifPresent(columnSpecs::addAll);
        if (fieldDefinitionContext.description() != null) {
            columnSpecs.add("COMMENT " + dbNameUtil.graphqlDescriptionToDBComment(fieldDefinitionContext.description().getText()));
        }
        columnDefinition.setColumnSpecs(columnSpecs);
        return Optional.of(columnDefinition);
    }

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext typeNameContext, GraphqlParser.DirectivesContext directivesContext) {
        if (manager.isEnum(typeNameContext.name().getText())) {
            return createEnumColDataType(typeNameContext);
        } else if (manager.isScalar(typeNameContext.name().getText())) {
            return createScalarColDataType(typeNameContext, directivesContext);
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeNameContext.getText()));
    }

    protected ColDataType createEnumColDataType(GraphqlParser.TypeNameContext typeNameContext) {
        ColDataType colDataType = new ColDataType();
        colDataType.setDataType("ENUM");

        colDataType.setArgumentsStringList(manager.getEnum(typeNameContext.name().getText()).map(enumTypeDefinitionContext -> enumTypeDefinitionContext.enumValueDefinitions()
                .enumValueDefinition().stream()
                .map(value -> dbNameUtil.stringValueToDBVarchar(value.getText())).collect(Collectors.toList())).orElse(Collections.emptyList()));

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
            case "String":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("VARCHAR"));
                argumentsStringList.add(length.map(argumentContext -> argumentContext.valueWithVariable().IntValue().getText()).orElse("255"));
                break;
            case "Boolean":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("BOOL"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Int":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("INT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Float":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("FLOAT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "BigInteger":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("BIGINT"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "BigDecimal":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("DECIMAL"));
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Date":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("DATE"));
                break;
            case "Time":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("TIME"));
                break;
            case "DateTime":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("DATETIME"));
                break;
            case "Timestamp":
                colDataType.setDataType(dataType.map(argumentContext -> dbNameUtil.graphqlStringValueToDBOption(argumentContext.valueWithVariable().StringValue().getText())).orElse("TIMESTAMP"));
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
            tableOptionsList.add("COMMENT " + dbNameUtil.graphqlDescriptionToDBComment(objectTypeDefinitionContext.description().getText()));
        }
        return tableOptionsList;
    }

    protected Optional<List<String>> directiveToTableOption(GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> tableDirective = getTableDirective(directivesContext);
        return tableDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToTableOption).collect(Collectors.toList()));
    }

    protected String argumentToTableOption(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return argumentContext.name().getText();
        } else {
            return dbNameUtil.directiveToTableOption(argumentContext.name().getText(), argumentContext.valueWithVariable().getText());
        }
    }

    protected Optional<List<String>> columnDirectiveToColumnSpecs(GraphqlParser.DirectivesContext directivesContext) {
        Optional<GraphqlParser.DirectiveContext> columnDirective = getColumnDirective(directivesContext);
        return columnDirective.map(directiveContext -> directiveContext.arguments().argument().stream().map(this::argumentToColumnSpecs).collect(Collectors.toList()));
    }

    protected String argumentToColumnSpecs(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return dbNameUtil.booleanDirectiveTocColumnDefinition(argumentContext.name().getText());
        } else {
            return dbNameUtil.directiveToColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().getText());
        }
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
