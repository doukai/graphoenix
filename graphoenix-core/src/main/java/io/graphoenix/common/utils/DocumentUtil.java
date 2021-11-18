package io.graphoenix.common.utils;

import com.pivovarit.function.ThrowingFunction;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public enum DocumentUtil {

    DOCUMENT_UTIL;

    public GraphqlParser.DocumentContext graphqlToDocument(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToDocument(charStream);
    }

    public GraphqlParser.DocumentContext graphqlToDocument(InputStream inputStream) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromStream(inputStream);
        return graphqlToDocument(charStream);
    }

    public Optional<GraphqlParser.DocumentContext> graphqlTryToDocument(InputStream inputStream) {
        ThrowingFunction<InputStream, CharStream, IOException> fromFileName = CharStreams::fromStream;
        return fromFileName.lift().apply(inputStream).map(this::graphqlToDocument);
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(InputStream inputStream) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromStream(inputStream);
        return graphqlToOperation(charStream);
    }

    public Optional<GraphqlParser.OperationDefinitionContext> graphqlTryToOperation(InputStream inputStream) {
        ThrowingFunction<InputStream, CharStream, IOException> fromFileName = CharStreams::fromStream;
        return fromFileName.lift().apply(inputStream).map(this::graphqlToOperation);
    }

    public GraphqlParser.DocumentContext graphqlFileToDocument(String graphqlFileName) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromFileName(graphqlFileName);
        return graphqlToDocument(charStream);
    }

    public Optional<GraphqlParser.DocumentContext> graphqlFileTryToDocument(String graphqlFileName) {
        ThrowingFunction<String, CharStream, IOException> fromFileName = CharStreams::fromFileName;
        return fromFileName.lift().apply(graphqlFileName).map(this::graphqlToDocument);
    }


    public GraphqlParser.DocumentContext graphqlPathToDocument(Path graphqlPath) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromPath(graphqlPath);
        return graphqlToDocument(charStream);
    }

    public Optional<GraphqlParser.DocumentContext> graphqlPathTryToDocument(Path graphqlPath) {
        ThrowingFunction<Path, CharStream, IOException> fromFileName = CharStreams::fromPath;
        return fromFileName.lift().apply(graphqlPath).map(this::graphqlToDocument);
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToOperation(charStream);
    }

    public GraphqlParser.DocumentContext graphqlToDocument(CharStream charStream) {
        return getGraphqlParser(charStream).document();
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(CharStream charStream) {
        return getGraphqlParser(charStream).operationDefinition();
    }

    public String getStringValue(TerminalNode stringValue) {
        return stringValue.getText().substring(1, stringValue.getText().length() - 1);
    }

    private GraphqlParser getGraphqlParser(CharStream charStream) {
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
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            }
        });
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        return parser;
    }
}
