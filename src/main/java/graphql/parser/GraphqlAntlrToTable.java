package graphql.parser;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlAntlrToTable {

    GraphqlAntlrRegister graphqlAntlrRegister;

    public GraphqlAntlrToTable(GraphqlAntlrRegister graphqlAntlrRegister) {
        this.graphqlAntlrRegister = graphqlAntlrRegister;
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
        return Optional.of(createTable(typeDefinitionContext.objectTypeDefinition()));
    }

    protected CreateTable createTable(GraphqlParser.ObjectTypeDefinitionContext ctx) {

        CreateTable createTable = new CreateTable();
        Table table = new Table();
        table.setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ctx.name().getText()));
        createTable.setColumnDefinitions(ctx.fieldsDefinition().fieldDefinition().stream().map(this::createColumn).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));

        return createTable;
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext ctx) {

        if (ctx.type().listType() != null) {
            return Optional.empty();
        }
//TODO
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ctx.name().getText()));

        if (ctx.type().typeName() != null) {
            columnDefinition.setColDataType(createColDataType(ctx.type().typeName()));

        } else if (ctx.type().nonNullType() != null) {
            columnDefinition.setColDataType(createColDataType(ctx.type().nonNullType().typeName()));
            List<String> columnSpecs = new ArrayList<>();
            columnSpecs.add("NOT NULL");
            columnDefinition.setColumnSpecs(columnSpecs);
        }

//        ctx.description();
//        ctx.directives();
//        ctx.argumentsDefinition();
        return Optional.of(columnDefinition);
    }

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext typeNameContext) {
        if (graphqlAntlrRegister.exist(typeNameContext.name().getText())) {
            switch (graphqlAntlrRegister.getDefinitionType(typeNameContext.name().getText()).toLowerCase()) {
                case "type":
                    return createTypeColDataType(typeNameContext);
                case "enum":
                    return createEnumColDataType(typeNameContext);
                default:
                    return null;
            }

        } else {
            return createDefaultScalarColDataType(typeNameContext);
        }
    }

    protected ColDataType createTypeColDataType(GraphqlParser.TypeNameContext typeNameContext) {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        colDataType.setDataType("INT");
        argumentsStringList.add("20");
        colDataType.setArgumentsStringList(argumentsStringList);

        return colDataType;
    }

    protected ColDataType createEnumColDataType(GraphqlParser.TypeNameContext typeNameContext) {
        ColDataType colDataType = new ColDataType();
        colDataType.setDataType("ENUM");
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
