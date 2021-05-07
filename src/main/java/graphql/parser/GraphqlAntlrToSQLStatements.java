package graphql.parser;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class GraphqlAntlrToSQLStatements {

    public Statements createStatements(GraphqlParser.DocumentContext ctx) {
        Statements statements = new Statements();
        statements.setStatements(ctx.definition().stream().map(this::createStatement).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        return statements;
    }

    protected Optional<? extends Statement> createStatement(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() != null) {

        } else if (definitionContext.typeSystemDefinition() != null) {
            return createCreateTable(definitionContext.typeSystemDefinition());
        }

        return Optional.empty();
    }


    protected Optional<CreateTable> createCreateTable(GraphqlParser.TypeSystemDefinitionContext ctx) {
        if (ctx.typeDefinition() != null) {
            return createCreateTable(ctx.typeDefinition());
        }
        return Optional.empty();
    }

    protected Optional<CreateTable> createCreateTable(GraphqlParser.TypeDefinitionContext ctx) {

        if (ctx.objectTypeDefinition() != null) {
            return Optional.of(createTable(ctx.objectTypeDefinition()));
        }
        return Optional.empty();
    }

    protected CreateTable createTable(GraphqlParser.ObjectTypeDefinitionContext ctx) {

        CreateTable createTable = new CreateTable();
        Table table = new Table();
        table.setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ctx.name().getText()));
        createTable.setColumnDefinitions(ctx.fieldsDefinition().fieldDefinition().stream().map(this::createColumnDefinition).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));

        return createTable;
    }

    protected Optional<ColumnDefinition> createColumnDefinition(GraphqlParser.FieldDefinitionContext ctx) {

        if (ctx.type().listType() != null) {
            return Optional.empty();
        }

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

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext ctx) {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();

        switch (ctx.name().getText()) {
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
