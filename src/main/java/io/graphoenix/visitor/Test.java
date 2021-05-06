package io.graphoenix.visitor;

import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Test {

    public static void main(String[] args){
        CodePointCharStream charStream;

        charStream = CharStreams.fromString("multiSourceReader");
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

    }
}
