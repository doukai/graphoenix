import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mygql.parser.*;
import io.graphonix.grantlr.manager.*;
import io.graphonix.grantlr.manager.impl.*;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class GraphqlTest {

    @Test
    void createType() throws IOException {
        IGraphqlOperationManager graphqlOperationManager = new GraphqlOperationManager();
        IGraphqlObjectManager graphqlObjectManager = new GraphqlObjectManager();
        IGraphqlFieldManager graphqlFieldManager = new GraphqlFieldManager();
        IGraphqlInputObjectManager graphqlInputObjectManager = new GraphqlInputObjectManager();
        IGraphqlInputValueManager graphqlInputValueManager = new GraphqlInputValueManager();
        IGraphqlEnumManager graphqlEnumManager = new GraphqlEnumManager();
        IGraphqlScalarManager graphqlScalarManager = new GraphqlScalarManager();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphqlOperationManager, graphqlObjectManager, graphqlFieldManager, graphqlInputObjectManager, graphqlInputValueManager, graphqlEnumManager, graphqlScalarManager);
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrManager);
        CodePointCharStream charStream;
        URL url = Resources.getResource("test.graphqls");
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
        graphqlAntlrManager.registerDocument(documentContext);
        List<CreateTable> tables = graphqlTypeToTable.createTables(documentContext);
        tables.forEach(createTable -> System.out.println(createTable.toString()));
    }

    @Test
    void convertQuery() throws IOException {
        IGraphqlOperationManager graphqlOperationManager = new GraphqlOperationManager();
        IGraphqlObjectManager graphqlObjectManager = new GraphqlObjectManager();
        IGraphqlFieldManager graphqlFieldManager = new GraphqlFieldManager();
        IGraphqlInputObjectManager graphqlInputObjectManager = new GraphqlInputObjectManager();
        IGraphqlInputValueManager graphqlInputValueManager = new GraphqlInputValueManager();
        IGraphqlEnumManager graphqlEnumManager = new GraphqlEnumManager();
        IGraphqlScalarManager graphqlScalarManager = new GraphqlScalarManager();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphqlOperationManager, graphqlObjectManager, graphqlFieldManager, graphqlInputObjectManager, graphqlInputValueManager, graphqlEnumManager, graphqlScalarManager);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        CodePointCharStream charStream;
        URL url = Resources.getResource("test.graphqls");
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
        graphqlAntlrManager.registerDocument(documentContext);
        List<Select> selects = graphqlQueryToSelect.createSelects(documentContext);
        selects.forEach(select -> System.out.println(select.toString()));
    }

    @Test
    void convertSelect() throws IOException {
        IGraphqlOperationManager graphqlOperationManager = new GraphqlOperationManager();
        IGraphqlObjectManager graphqlObjectManager = new GraphqlObjectManager();
        IGraphqlFieldManager graphqlFieldManager = new GraphqlFieldManager();
        IGraphqlInputObjectManager graphqlInputObjectManager = new GraphqlInputObjectManager();
        IGraphqlInputValueManager graphqlInputValueManager = new GraphqlInputValueManager();
        IGraphqlEnumManager graphqlEnumManager = new GraphqlEnumManager();
        IGraphqlScalarManager graphqlScalarManager = new GraphqlScalarManager();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphqlOperationManager, graphqlObjectManager, graphqlFieldManager, graphqlInputObjectManager, graphqlInputValueManager, graphqlEnumManager, graphqlScalarManager);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
        CodePointCharStream charStream;
        URL url = Resources.getResource("test.graphqls");
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
        graphqlAntlrManager.registerDocument(documentContext);
        Statements statements = graphqlMutationToStatements.createStatements(documentContext);
        statements.getStatements().forEach(statement -> System.out.println(statement.toString()));
    }
}
