package io.graphoenix.grantlr.common.utils;

import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;

public enum DocumentUtil {

    DOCUMENT_UTIL;

    public GraphqlParser.DocumentContext graphqlToDocument(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
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
        return parser.document();
    }
}
