package io.graphoenix.visitor;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.parser.*;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Test {


    public static void main(String[] args) throws IOException {
        GraphqlAntlrRegister graphqlAntlrRegister = new GraphqlAntlrRegister();
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrRegister);
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrRegister);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrRegister, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrRegister, graphqlQueryToSelect);
        CodePointCharStream charStream;


        URL url = Resources.getResource("static/test.graphqls");
        String sdl = Resources.toString(url, Charsets.UTF_8);

        charStream = CharStreams.fromString(sdl);
        GraphqlLexer lexer = new GraphqlLexer(charStream);

        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {

            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        GraphqlParser parser = new GraphqlParser(tokens);
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

        GraphqlParser.DocumentContext documentContext = parser.document();
        graphqlAntlrRegister.registerDocument(documentContext);
        List<CreateTable> tables = graphqlTypeToTable.createTables(documentContext);
        List<Select> selects = graphqlQueryToSelect.createSelects(documentContext);
        Statements statements = graphqlMutationToStatements.createStatements(documentContext);


        tables.forEach(createTable -> System.out.println(createTable.toString()));
        selects.forEach(select -> System.out.println(select.toString()));
        statements.getStatements().forEach(statement -> System.out.println(statement.toString()));


    }
}
