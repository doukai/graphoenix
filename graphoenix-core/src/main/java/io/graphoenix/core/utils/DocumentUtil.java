package io.graphoenix.core.utils;

import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

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

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(InputStream inputStream) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromStream(inputStream);
        return graphqlToOperation(charStream);
    }

    public GraphqlParser.DocumentContext graphqlFileToDocument(File graphqlFile) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromFileName(graphqlFile.getPath());
        return graphqlToDocument(charStream);
    }

    public GraphqlParser.DocumentContext graphqlPathToDocument(Path graphqlPath) throws IOException {
        CharStream charStream;
        charStream = CharStreams.fromPath(graphqlPath);
        return graphqlToDocument(charStream);
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToOperation(charStream);
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(String graphql, Map<String, String> variables) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToOperation(charStream, variables);
    }

    public GraphqlParser.DocumentContext graphqlToDocument(CharStream charStream) {
        return getGraphqlParser(charStream).document();
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(CharStream charStream) {
        return getGraphqlParser(charStream).operationDefinition();
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(CharStream charStream, Map<String, String> variables) {
        //TODO
        GraphqlParser.OperationDefinitionContext operationDefinitionContext = getGraphqlParser(charStream).operationDefinition();

        operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .map(variableDefinitionContext -> variableDefinitionContext.type().typeName());

        operationDefinitionContext.selectionSet().selection().stream()
                .forEach(selectionContext -> selectionContext.field().arguments());

        return operationDefinitionContext;
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
