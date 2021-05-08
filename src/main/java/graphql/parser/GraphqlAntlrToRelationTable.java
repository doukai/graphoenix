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

public class GraphqlAntlrToRelationTable {

    GraphqlAntlrRegister graphqlAntlrRegister;

    public GraphqlAntlrToRelationTable(GraphqlAntlrRegister graphqlAntlrRegister) {
        this.graphqlAntlrRegister = graphqlAntlrRegister;
    }

    public List<CreateTable> createRelationTables(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream()
                .map(this::createRelationTables).collect(Collectors.toList()).stream()
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()).stream()
                .flatMap(List::stream).collect(Collectors.toList());
    }

    protected Optional<List<CreateTable>> createRelationTables(GraphqlParser.DefinitionContext definitionContext) {

        if (definitionContext.typeSystemDefinition() == null) {
            return Optional.empty();
        }
        return createRelationTables(definitionContext.typeSystemDefinition());
    }

    protected Optional<List<CreateTable>> createRelationTables(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.typeDefinition() == null) {
            return Optional.empty();
        }
        return createRelationTables(typeSystemDefinitionContext.typeDefinition());
    }

    protected Optional<List<CreateTable>> createRelationTables(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.objectTypeDefinition() == null) {
            return Optional.empty();
        }
        return createRelationTables(typeDefinitionContext.objectTypeDefinition());
    }

    protected Optional<List<CreateTable>> createRelationTables(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        if (objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().anyMatch(fieldDefinitionContext -> fieldDefinitionContext.type().listType() != null)) {

            return Optional.of(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                    .filter(fieldDefinitionContext -> fieldDefinitionContext.type().listType() != null)
                    .map(fieldDefinitionContext -> createRelationTable(objectTypeDefinitionContext, fieldDefinitionContext))
                    .collect(Collectors.toList()));

        }

        return Optional.empty();
    }

    protected CreateTable createRelationTable(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        CreateTable createTable = new CreateTable();
        Table table = new Table();
        table.setName(objectTypeDefinitionContext.name().getText() + "_" + fieldDefinitionContext.name().getText());
        createTable.setTable(table);

        ColumnDefinition sourceColumnDefinition = createSourceColumn();
        ColumnDefinition targetColumnDefinition = createTargetColumn(fieldDefinitionContext.name().getText());

        createTable.setColumnDefinitions(Arrays.asList(targetColumnDefinition, sourceColumnDefinition));
        List<String> tableOptionsStrings = new ArrayList<>(Arrays.asList("ENGINE=InnoDB", "CHARSET=utf8"));
        createTable.setIfNotExists(true);
        createTable.setTableOptionsStrings(tableOptionsStrings);
        return createTable;
    }

    protected ColumnDefinition createSourceColumn() {

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName("id");

        List<String> columnSpecs = new ArrayList<>();
        columnSpecs.add("AUTO_INCREMENT");
        columnSpecs.add("PRIMARY KEY");
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


    protected ColumnDefinition createTargetColumn(String fieldName) {

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName) + "_" + "id");

        List<String> columnSpecs = new ArrayList<>();
        columnSpecs.add("AUTO_INCREMENT");
        columnSpecs.add("PRIMARY KEY");
        columnDefinition.setColumnSpecs(columnSpecs);
        columnDefinition.setColDataType(createTargetColDataType());
        return columnDefinition;
    }

    protected ColDataType createTargetColDataType() {

        ColDataType colDataType = new ColDataType();
        List<String> argumentsStringList = new ArrayList<>();
        colDataType.setDataType("INT");
        argumentsStringList.add("20");
        colDataType.setArgumentsStringList(argumentsStringList);
        return colDataType;
    }
}
