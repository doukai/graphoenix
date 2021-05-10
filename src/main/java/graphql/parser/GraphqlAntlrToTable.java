package graphql.parser;

import com.google.common.base.CaseFormat;
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

public class GraphqlAntlrToTable {

    private final GraphqlAntlrRegister graphqlAntlrRegister;

    private final String[] scalarType = {"ID", "Boolean", "String", "Float", "Int"};

    public GraphqlAntlrToTable(GraphqlAntlrRegister graphqlAntlrRegister) {
        this.graphqlAntlrRegister = graphqlAntlrRegister;
    }

    public List<CreateTable> createTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createTable).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    protected boolean isScalar(String objectName) {

        return Arrays.asList(scalarType).contains(objectName);
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
        return Optional.of(createTable(typeDefinitionContext.objectTypeDefinition()));
    }

    protected CreateTable createTable(GraphqlParser.ObjectTypeDefinitionContext ctx) {

        CreateTable createTable = new CreateTable();
        Table table = new Table();
        table.setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ctx.name().getText()));
        createTable.setColumnDefinitions(ctx.fieldsDefinition().fieldDefinition().stream().map(this::createColumn).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));

        return createTable;
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return createColumn(fieldDefinitionContext, false, false);
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean list, boolean nonNull) {

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldDefinitionContext.name().getText()));

        if (fieldDefinitionContext.type().nonNullType() != null) {

            return createColumn(fieldDefinitionContext, list, true);

        } else if (fieldDefinitionContext.type().listType() != null) {

            return createColumn(fieldDefinitionContext, true, nonNull);
        } else if (fieldDefinitionContext.type().typeName() != null) {
            ColDataType colDataType = null;

            if (graphqlAntlrRegister.exist(fieldDefinitionContext.type().typeName().name().getText())) {
                String definitionType = graphqlAntlrRegister.getDefinitionType(fieldDefinitionContext.type().typeName().name().getText()).toLowerCase();
                if (definitionType.equals("type") && list) {
                    return Optional.empty();
                } else if (definitionType.equals("type")) {
                    colDataType = createTypeColDataType(fieldDefinitionContext.type().typeName());
                } else if (definitionType.equals("enum")) {
                    colDataType = createEnumColDataType(fieldDefinitionContext.type().typeName(), list);
                }
            } else if (isScalar(fieldDefinitionContext.type().typeName().name().getText()) && list) {
                return Optional.empty();
            } else if (isScalar(fieldDefinitionContext.type().typeName().name().getText())) {
                colDataType = createDefaultScalarColDataType(fieldDefinitionContext.type().typeName());
            }
            columnDefinition.setColDataType(colDataType);
            List<String> columnSpecs = new ArrayList<>();
            if (nonNull) {
                columnSpecs.add("NOT NULL");
            }
            columnDefinition.setColumnSpecs(columnSpecs);
            return Optional.of(columnDefinition);

        }
//        ctx.description();
//        ctx.directives();
//        ctx.argumentsDefinition();
        return Optional.empty();
    }

    protected ColDataType createTypeColDataType(GraphqlParser.TypeNameContext typeNameContext) {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        colDataType.setDataType("INT");
        argumentsStringList.add("20");
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
                .map(value -> "'" + value + "'")
                .collect(Collectors.toList()));

        return colDataType;
    }

    protected ColDataType createDefaultScalarColDataType(GraphqlParser.TypeNameContext typeNameContext) {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        switch (typeNameContext.name().getText()) {
            case "ID":
            case "Int":
                colDataType.setDataType("INT");
                argumentsStringList.add("20");
                break;
            case "Boolean":
                colDataType.setDataType("BOOL");
                break;
            case "String":
                colDataType.setDataType("VARCHAR");
                argumentsStringList.add("50");
                break;
            case "Float":
                colDataType.setDataType("FLOAT");
                argumentsStringList.add("20");
                argumentsStringList.add("2");
                break;
        }

        colDataType.setArgumentsStringList(argumentsStringList);
        return colDataType;
    }
}
