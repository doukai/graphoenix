//import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GraphqlTest {

    @Test
    void createType() throws IOException {
//        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager();
//        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrManager);
//        CodePointCharStream charStream;
//        URL url = Resources.getResource("test.graphqls");
//        String sdl = Resources.toString(url, Charsets.UTF_8);
//        charStream = CharStreams.fromString(sdl);
//        GraphqlLexer lexer = new GraphqlLexer(charStream);
//        lexer.removeErrorListeners();
//        lexer.addErrorListener(new BaseErrorListener() {
//            @Override
//            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
//
//            }
//        });
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        GraphqlParser parser = new GraphqlParser(tokens);
//        parser.removeErrorListeners();
//        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
//        GraphqlParser.DocumentContext documentContext = parser.document();
//        graphqlAntlrManager.registerDocument(documentContext);
//        List<CreateTable> tables = graphqlTypeToTable.createTables(documentContext);
//        tables.forEach(createTable -> System.out.println(createTable.toString()));
    }

    @Test
    void convertQuery() throws IOException {
//        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager();
//        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
//        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
//        CodePointCharStream charStream;
//        URL url = Resources.getResource("test.graphqls");
//        String sdl = Resources.toString(url, Charsets.UTF_8);
//        charStream = CharStreams.fromString(sdl);
//        GraphqlLexer lexer = new GraphqlLexer(charStream);
//        lexer.removeErrorListeners();
//        lexer.addErrorListener(new BaseErrorListener() {
//            @Override
//            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
//
//            }
//        });
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        GraphqlParser parser = new GraphqlParser(tokens);
//        parser.removeErrorListeners();
//        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
//        GraphqlParser.DocumentContext documentContext = parser.document();
//        graphqlAntlrManager.registerDocument(documentContext);
//        List<Select> selects = graphqlQueryToSelect.createSelects(documentContext);
//        selects.forEach(select -> System.out.println(select.toString()));
    }

    @Test
    void convertMutation() throws IOException {
//        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager();
//        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
//        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
//        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
//        CodePointCharStream charStream;
//        URL url = Resources.getResource("test.graphqls");
//        String sdl = Resources.toString(url, Charsets.UTF_8);
//        charStream = CharStreams.fromString(sdl);
//        GraphqlLexer lexer = new GraphqlLexer(charStream);
//        lexer.removeErrorListeners();
//        lexer.addErrorListener(new BaseErrorListener() {
//            @Override
//            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
//
//            }
//        });
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        GraphqlParser parser = new GraphqlParser(tokens);
//        parser.removeErrorListeners();
//        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
//        GraphqlParser.DocumentContext documentContext = parser.document();
//        graphqlAntlrManager.registerDocument(documentContext);
//        Statements statements = graphqlMutationToStatements.createStatements(documentContext);
//        statements.getStatements().forEach(statement -> System.out.println(statement.toString()));
    }
}
