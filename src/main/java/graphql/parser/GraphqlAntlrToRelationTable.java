package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlAntlrToRelationTable extends GraphqlAntlrToTable {

    public GraphqlAntlrToRelationTable(GraphqlAntlrRegister graphqlAntlrRegister) {

        super(graphqlAntlrRegister);
    }

    @Override
    public List<CreateTable> createTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream()
                .map(this::createTables).collect(Collectors.toList()).stream()
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()).stream()
                .flatMap(List::stream).collect(Collectors.toList());
    }

    protected Optional<List<CreateTable>> createTables(GraphqlParser.DefinitionContext definitionContext) {

        if (definitionContext.typeSystemDefinition() == null) {
            return Optional.empty();
        }
        return createTables(definitionContext.typeSystemDefinition());
    }

    protected Optional<List<CreateTable>> createTables(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.typeDefinition() == null) {
            return Optional.empty();
        }
        return createTables(typeSystemDefinitionContext.typeDefinition());
    }

    protected Optional<List<CreateTable>> createTables(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.objectTypeDefinition() == null) {
            return Optional.empty();
        }
        return createTables(typeDefinitionContext.objectTypeDefinition());
    }

    protected Optional<List<CreateTable>> createTables(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        if (objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .anyMatch(fieldDefinitionContext -> fieldDefinitionContext.type().listType() != null)) {

            return Optional.of(
                    objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                            .map(fieldDefinitionContext -> createTables(objectTypeDefinitionContext, fieldDefinitionContext))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList())
            );
        }

        return Optional.empty();
    }

    protected Optional<CreateTable> createTables(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        if (fieldDefinitionContext.type().typeName() != null) {
            return Optional.empty();
        } else if (fieldDefinitionContext.type().listType() != null) {
            return createTables(
                    objectTypeDefinitionContext,
                    fieldDefinitionContext,
                    fieldDefinitionContext.type().listType()
            );
        } else if (fieldDefinitionContext.type().nonNullType() != null) {
            if (fieldDefinitionContext.type().nonNullType().typeName() != null) {
                return Optional.empty();
            } else if (fieldDefinitionContext.type().nonNullType().listType() != null) {
                return createTables(
                        objectTypeDefinitionContext,
                        fieldDefinitionContext,
                        fieldDefinitionContext.type().nonNullType().listType()
                );
            }
        }
        return Optional.empty();
    }

    protected Optional<CreateTable> createTables(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                 GraphqlParser.ListTypeContext listTypeContext) {

        if (listTypeContext.type().nonNullType() != null && isEnum(listTypeContext.type().nonNullType().typeName().name().getText())) {
            return Optional.empty();
        } else if (listTypeContext.type().typeName() != null && isEnum(listTypeContext.type().typeName().name().getText())) {
            return Optional.empty();
        }

        CreateTable createTable = new CreateTable();
        Table table = new Table();
        table.setName(DBNameConverter.INSTANCE.graphqlTypeFieldNameToRelationTableName(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()));

        ColumnDefinition sourceColumnDefinition = createSourceColumn();

        ColumnDefinition targetColumnDefinition = null;

        if (listTypeContext.type().nonNullType() != null) {
            targetColumnDefinition = createTargetColumn(fieldDefinitionContext, listTypeContext.type().nonNullType().typeName(), true);
        } else if (listTypeContext.type().typeName() != null) {
            targetColumnDefinition = createTargetColumn(fieldDefinitionContext, listTypeContext.type().typeName(), false);
        }

        createTable.setTable(table);
        createTable.setColumnDefinitions(Arrays.asList(sourceColumnDefinition, targetColumnDefinition));
        createTable.setIfNotExists(true);
        createTable.setTableOptionsStrings(createRelationTableOption(objectTypeDefinitionContext, fieldDefinitionContext));
        return Optional.of(createTable);
    }

    protected ColumnDefinition createSourceColumn() {

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(DBNameConverter.INSTANCE.graphqlTypeFieldNameToColumnName("id"));
        List<String> columnSpecs = new ArrayList<>();
        columnSpecs.add("NOT NULL");
        columnDefinition.setColumnSpecs(columnSpecs);
        columnDefinition.setColDataType(createSourceColDataType());
        return columnDefinition;
    }

    protected ColDataType createSourceColDataType() {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        colDataType.setDataType("INT");
        argumentsStringList.add("20");
        colDataType.setArgumentsStringList(argumentsStringList);
        return colDataType;
    }

    protected ColumnDefinition createTargetColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.TypeNameContext typeNameContext, boolean nonNull) {

        ColumnDefinition columnDefinition = new ColumnDefinition();

        if (isObject(typeNameContext.name().getText())) {
            columnDefinition.setColumnName(DBNameConverter.INSTANCE.graphqlTypeFieldNameToRelationColumnName(fieldDefinitionContext.name().getText(), "id"));
        } else {
            columnDefinition.setColumnName(DBNameConverter.INSTANCE.graphqlTypeFieldNameToColumnName(fieldDefinitionContext.name().getText()));
        }

        columnDefinition.setColDataType(createColDataType(typeNameContext, fieldDefinitionContext.directives(), false));
        List<String> columnSpecs = new ArrayList<>();
        if (nonNull) {
            columnSpecs.add("NOT NULL");
        }
        columnDefinition.setColumnSpecs(columnSpecs);
        return columnDefinition;
    }
}
