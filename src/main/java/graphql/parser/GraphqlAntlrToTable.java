package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    protected ColDataType createColDataType(GraphqlParser.TypeNameContext typeNameContext, boolean list) {

        if (graphqlAntlrRegister.exist(typeNameContext.name().getText())) {
            String definitionType = graphqlAntlrRegister.getDefinitionType(typeNameContext.name().getText()).toLowerCase();
            if (definitionType.equals("type")) {
                return createTypeColDataType(typeNameContext);
            } else if (definitionType.equals("enum")) {
                return createEnumColDataType(typeNameContext, list);
            }
        } else if (isScalar(typeNameContext.name().getText())) {
            return createDefaultScalarColDataType(typeNameContext);
        }
        //TODO
        return null;
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
