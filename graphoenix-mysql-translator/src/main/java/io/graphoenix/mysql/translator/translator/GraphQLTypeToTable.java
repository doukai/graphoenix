package io.graphoenix.mysql.translator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.mysql.translator.statement.CreateDataBase;
import io.graphoenix.mysql.translator.utils.DBNameUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.truncate.Truncate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.DEFINITION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_DEFINITION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class GraphQLTypeToTable {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final DBNameUtil dbNameUtil;

    @Inject
    public GraphQLTypeToTable(IGraphQLDocumentManager manager, PackageManager packageManager, DBNameUtil dbNameUtil) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.dbNameUtil = dbNameUtil;
    }

    public String selectColumnsSQL() {
        return selectColumns().toString();
    }

    public Select selectColumns() {
        return new Select()
                .withSelectBody(
                        new PlainSelect()
                                .withSelectItems(
                                        List.of(
                                                new SelectExpressionItem(new Column("table_name")),
                                                new SelectExpressionItem(new Column("column_name"))
                                        )
                                )
                                .withFromItem(new Table("COLUMNS").withSchemaName("information_schema"))
                                .withWhere(
                                        new EqualsTo()
                                                .withLeftExpression(new Column("table_schema"))
                                                .withRightExpression(new Function().withName("DATABASE"))
                                )
                );
    }

    public Stream<String> truncateIntrospectionObjectTablesSQL() {
        return truncateIntrospectionObjectTables().map(Truncate::toString);
    }

    public Stream<Truncate> truncateIntrospectionObjectTables() {
        return manager.getObjects()
                .filter(packageManager::isLocalPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .filter(objectTypeDefinitionContext -> objectTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                .map(this::truncateObjectTable);
    }

    public Truncate truncateObjectTable(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        Table table = dbNameUtil.typeToTable(objectTypeDefinitionContext.name().getText());
        return new Truncate().withTable(table);
    }

    public String mergeSchemaSQL(String schemaName) {
        return mergeSchema(schemaName).toString();
    }

    public CreateDataBase mergeSchema(String schemaName) {
        return new CreateDataBase().withExist(false).withDataBaseName(schemaName);
    }

    public Stream<String> mergeTablesSQL(List<Tuple2<String, String>> existsColumnNameList) {
        return manager.getObjects()
                .filter(packageManager::isLocalPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .map(objectTypeDefinitionContext ->
                        alterTable(objectTypeDefinitionContext, existsColumnNameList)
                                .map(Alter::toString)
                                .orElseGet(() -> createTable(objectTypeDefinitionContext).toString())
                );
    }

    public Stream<String> createTablesSQL() {
        return manager.getObjects()
                .filter(packageManager::isLocalPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .map(this::createTable)
                .map(CreateTable::toString);
    }

    public Stream<CreateTable> createTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createTable).flatMap(Optional::stream);
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
        Table table = dbNameUtil.typeToTable(objectTypeDefinitionContext.name().getText());
        createTable.setTable(table);
        createTable.setColumnDefinitions(
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(manager::isNotInvokeField)
                        .filter(manager::isNotFetchField)
                        .filter(manager::isNotFunctionField)
                        .filter(manager::isNotConnectionField)
                        .map(this::createColumn)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList())
        );
        createTable.setIfNotExists(true);
        createTable.setTableOptionsStrings(createTableOption(objectTypeDefinitionContext));
        return createTable;
    }

    public Stream<String> alterTablesSQL(List<Tuple2<String, String>> existsColumnNameList) {
        return manager.getObjects()
                .filter(packageManager::isLocalPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::isNotContainerType)
                .map(objectTypeDefinitionContext -> alterTable(objectTypeDefinitionContext, existsColumnNameList))
                .flatMap(Optional::stream)
                .map(Alter::toString);
    }

    public Stream<Alter> alterTable(GraphqlParser.DocumentContext documentContext, List<Tuple2<String, String>> existsColumnNameList) {
        return documentContext.definition().stream().map(definitionContext -> alterTable(definitionContext, existsColumnNameList)).flatMap(Optional::stream);
    }

    protected Optional<Alter> alterTable(GraphqlParser.DefinitionContext definitionContext, List<Tuple2<String, String>> existsColumnNameList) {
        if (definitionContext.typeSystemDefinition() == null) {
            throw new GraphQLErrors(DEFINITION_NOT_EXIST);
        }
        return alterTable(definitionContext.typeSystemDefinition(), existsColumnNameList);
    }

    protected Optional<Alter> alterTable(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext, List<Tuple2<String, String>> existsColumnNameList) {
        if (typeSystemDefinitionContext.typeDefinition() == null) {
            throw new GraphQLErrors(TYPE_DEFINITION_NOT_EXIST);
        }
        return alterTable(typeSystemDefinitionContext.typeDefinition(), existsColumnNameList);
    }

    protected Optional<Alter> alterTable(GraphqlParser.TypeDefinitionContext typeDefinitionContext, List<Tuple2<String, String>> existsColumnNameList) {
        if (typeDefinitionContext.objectTypeDefinition() == null) {
            return Optional.empty();
        }
        if (manager.isOperation(typeDefinitionContext.objectTypeDefinition().name().getText())) {
            return Optional.empty();
        }
        return alterTable(typeDefinitionContext.objectTypeDefinition(), existsColumnNameList);
    }

    protected Optional<Alter> alterTable(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, List<Tuple2<String, String>> existsColumnNameList) {
        Table table = dbNameUtil.typeToTable(objectTypeDefinitionContext.name().getText());
        if (existsColumnNameList.stream().noneMatch(tuple2 -> dbNameUtil.nameToDBEscape(tuple2._1()).equals(table.getName()))) {
            return Optional.empty();
        }
        return Optional.of(
                new Alter()
                        .withTable(table)
                        .withAlterExpressions(
                                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                        .filter(manager::isNotInvokeField)
                                        .filter(manager::isNotFetchField)
                                        .filter(manager::isNotFunctionField)
                                        .filter(manager::isNotConnectionField)
                                        .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals("ID"))
                                        .map(this::createColumn)
                                        .flatMap(Optional::stream)
                                        .map(columnDefinition ->
                                                alterColumn(
                                                        columnDefinition,
                                                        existsColumnNameList.stream()
                                                                .anyMatch(tuple2 ->
                                                                        dbNameUtil.nameToDBEscape(tuple2._1()).equals(table.getName()) &&
                                                                                dbNameUtil.nameToDBEscape(tuple2._2()).equals(columnDefinition.getColumnName())
                                                                )
                                                )
                                        )
                                        .collect(Collectors.toList())
                        )
        );
    }

    protected AlterExpression alterColumn(ColumnDefinition columnDefinition, boolean exists) {
        AlterExpression alterExpression = new AlterExpression()
                .withOperation(exists ? AlterOperation.MODIFY : AlterOperation.ADD);
        alterExpression.addColDataType(
                new AlterExpression.ColumnDataType(false)
                        .withColumnName(columnDefinition.getColumnName())
                        .withColDataType(columnDefinition.getColDataType())
                        .withColumnSpecs(columnDefinition.getColumnSpecs())
        );
        return alterExpression;
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
        columnDefinition.setColDataType(createColDataType(fieldDefinitionContext));
        List<String> columnSpecs = new ArrayList<>();
        if (typeNameContext.name().getText().equals("ID")) {
            columnSpecs.add("PRIMARY KEY");
        }
        if (nonNull) {
            columnSpecs.add("NOT NULL");
        } else {
            if (typeNameContext.name().getText().equals("Date") ||
                    typeNameContext.name().getText().equals("Time") ||
                    typeNameContext.name().getText().equals("DateTime") ||
                    typeNameContext.name().getText().equals("Timestamp")) {
                columnSpecs.add("NULL");
            }
        }
        Optional<String> defaultValue = getDataTypeDirective(fieldDefinitionContext.directives())
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("default")).findFirst())
                .map(argumentContext -> dbNameUtil.directiveToColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().getText()));

        if (defaultValue.isPresent()) {
            columnSpecs.add(defaultValue.get());
        } else {
            if (typeNameContext.name().getText().equals("Date") ||
                    typeNameContext.name().getText().equals("Time") ||
                    typeNameContext.name().getText().equals("DateTime") ||
                    typeNameContext.name().getText().equals("Timestamp")) {
                columnSpecs.add("DEFAULT NULL");
            }
        }

        Optional<GraphqlParser.DirectiveContext> dataTypeDirective = getDataTypeDirective(fieldDefinitionContext.directives());
        Optional<GraphqlParser.ArgumentContext> autoIncrement = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("autoIncrement")).findFirst());

        if (autoIncrement.isPresent()) {
            columnSpecs.add("AUTO_INCREMENT");
        }

        if (fieldDefinitionContext.description() != null) {
            columnSpecs.add("COMMENT " + dbNameUtil.graphqlDescriptionToDBComment(fieldDefinitionContext.description().getText()));
        }
        columnDefinition.setColumnSpecs(columnSpecs);
        return Optional.of(columnDefinition);
    }

    protected ColDataType createColDataType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (manager.isEnum(fieldTypeName)) {
            return createEnumColDataType(fieldDefinitionContext);
        } else if (manager.isScalar(fieldTypeName)) {
            return createScalarColDataType(fieldDefinitionContext);
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
    }

    protected ColDataType createEnumColDataType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        ColDataType colDataType = new ColDataType();
        colDataType.setDataType("ENUM");

        colDataType.setArgumentsStringList(manager.getEnum(manager.getFieldTypeName(fieldDefinitionContext.type())).map(enumTypeDefinitionContext -> enumTypeDefinitionContext.enumValueDefinitions()
                .enumValueDefinition().stream()
                .map(value -> dbNameUtil.stringValueToDBVarchar(value.getText())).collect(Collectors.toList())).orElse(Collections.emptyList()));

        return colDataType;
    }

    protected ColDataType createScalarColDataType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        Optional<GraphqlParser.DirectiveContext> dataTypeDirective = getDataTypeDirective(fieldDefinitionContext.directives());
        Optional<GraphqlParser.ArgumentContext> length = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("length")).findFirst());
        Optional<GraphqlParser.ArgumentContext> decimals = dataTypeDirective.flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("decimals")).findFirst());

        List<String> argumentsStringList = new ArrayList<>();
        ColDataType colDataType = new ColDataType();
        String fieldTypeName = manager.getDataTypeName(fieldDefinitionContext).orElse(manager.getFieldTypeName(fieldDefinitionContext.type()));
        switch (fieldTypeName) {
            case "ID":
            case "String":
                colDataType.setDataType("VARCHAR");
                argumentsStringList.add(length.map(argumentContext -> argumentContext.valueWithVariable().IntValue().getText()).orElse("255"));
                break;
            case "Boolean":
                colDataType.setDataType("BOOL");
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Int":
                colDataType.setDataType("INT");
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Float":
                colDataType.setDataType("FLOAT");
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "BigInteger":
                colDataType.setDataType("BIGINT");
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "BigDecimal":
                colDataType.setDataType("DECIMAL");
                length.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                decimals.ifPresent(argumentContext -> argumentsStringList.add(argumentContext.valueWithVariable().IntValue().getText()));
                break;
            case "Date":
                colDataType.setDataType("DATE");
                break;
            case "Time":
                colDataType.setDataType("TIME");
                break;
            case "DateTime":
                colDataType.setDataType("DATETIME");
                break;
            case "Timestamp":
                colDataType.setDataType("TIMESTAMP");
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

    protected Optional<GraphqlParser.DirectiveContext> getTableDirective(GraphqlParser.DirectivesContext directivesContext) {
        return Stream.ofNullable(directivesContext).map(GraphqlParser.DirectivesContext::directive).flatMap(Collection::stream).filter(directiveContext -> directiveContext.name().getText().equals("table")).findFirst();
    }

    protected Optional<GraphqlParser.DirectiveContext> getDataTypeDirective(GraphqlParser.DirectivesContext directivesContext) {
        return Stream.ofNullable(directivesContext).map(GraphqlParser.DirectivesContext::directive).flatMap(Collection::stream).filter(directiveContext -> directiveContext.name().getText().equals("dataType")).findFirst();
    }
}
