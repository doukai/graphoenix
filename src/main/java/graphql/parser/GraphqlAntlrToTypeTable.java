package graphql.parser;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlAntlrToTypeTable extends GraphqlAntlrToTable {

    public GraphqlAntlrToTypeTable(GraphqlAntlrRegister graphqlAntlrRegister) {
        super(graphqlAntlrRegister);
    }

    @Override
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

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        Optional<ColumnDefinition> columnDefinition = createColumn(fieldDefinitionContext.type(), false, false);

        columnDefinition.ifPresent(presentColumnDefinition -> presentColumnDefinition.setColumnName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldDefinitionContext.name().getText())));

        return columnDefinition;
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.TypeContext typeContext, boolean list, boolean nonNull) {

        if (typeContext.typeName() != null) {
            return createColumn(typeContext.typeName(), list, false);
        } else if (typeContext.listType() != null) {
            createColumn(typeContext.listType().type(), true, nonNull);
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return createColumn(typeContext.nonNullType().typeName(), list, true);
            } else if (typeContext.nonNullType().listType() != null) {
                createColumn(typeContext.nonNullType().listType().type(), true, true);
            }
        }
        return Optional.empty();
    }

    protected Optional<ColumnDefinition> createColumn(GraphqlParser.TypeNameContext typeNameContext, boolean list, boolean nonNull) {

        ColumnDefinition columnDefinition = new ColumnDefinition();
        if (list && !isEnum(typeNameContext.name().getText())) {
            return Optional.empty();
        }
        columnDefinition.setColDataType(createColDataType(typeNameContext, list));
        List<String> columnSpecs = new ArrayList<>();
        if (nonNull) {
            columnSpecs.add("NOT NULL");
        }
        columnDefinition.setColumnSpecs(columnSpecs);
        return Optional.of(columnDefinition);
    }
}
