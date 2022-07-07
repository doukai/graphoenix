package io.graphoenix.core.utils;

import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static io.graphoenix.core.error.GraphQLErrorType.SYNTAX_ERROR;

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

    public GraphqlParser.SelectionContext graphqlToSelection(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToSelection(charStream);
    }

    public GraphqlParser.SelectionSetContext graphqlToSelectionSet(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToSelectionSet(charStream);
    }

    public GraphqlParser.ObjectTypeDefinitionContext graphqlToObjectTypeDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToObjectTypeDefinition(charStream);
    }

    public GraphqlParser.InterfaceTypeDefinitionContext graphqlToInterfaceTypeDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToInterfaceTypeDefinition(charStream);
    }

    public GraphqlParser.EnumTypeDefinitionContext graphqlToEnumTypeDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToEnumTypeDefinition(charStream);
    }

    public GraphqlParser.InputObjectTypeDefinitionContext graphqlToInputObjectTypeDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToInputObjectTypeDefinition(charStream);
    }

    public GraphqlParser.FieldDefinitionContext graphqlToFieldDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToFieldDefinition(charStream);
    }

    public GraphqlParser.EnumValueDefinitionContext graphqlToEnumValueDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToEnumValueDefinition(charStream);
    }

    public GraphqlParser.InputValueDefinitionContext graphqlToInputValueDefinition(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return graphqlToInputValueDefinition(charStream);
    }

    public GraphqlParser.DocumentContext graphqlToDocument(CharStream charStream) {
        return getGraphqlParser(charStream).document();
    }

    public GraphqlParser.OperationDefinitionContext graphqlToOperation(CharStream charStream) {
        return getGraphqlParser(charStream).operationDefinition();
    }

    public GraphqlParser.SelectionContext graphqlToSelection(CharStream charStream) {
        return getGraphqlParser(charStream).selection();
    }

    public GraphqlParser.SelectionSetContext graphqlToSelectionSet(CharStream charStream) {
        return getGraphqlParser(charStream).selectionSet();
    }

    public GraphqlParser.ObjectTypeDefinitionContext graphqlToObjectTypeDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).objectTypeDefinition();
    }

    public GraphqlParser.InterfaceTypeDefinitionContext graphqlToInterfaceTypeDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).interfaceTypeDefinition();
    }

    public GraphqlParser.EnumTypeDefinitionContext graphqlToEnumTypeDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).enumTypeDefinition();
    }

    public GraphqlParser.InputObjectTypeDefinitionContext graphqlToInputObjectTypeDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).inputObjectTypeDefinition();
    }

    public GraphqlParser.FieldDefinitionContext graphqlToFieldDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).fieldDefinition();
    }

    public GraphqlParser.EnumValueDefinitionContext graphqlToEnumValueDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).enumValueDefinition();
    }

    public GraphqlParser.InputValueDefinitionContext graphqlToInputValueDefinition(CharStream charStream) {
        return getGraphqlParser(charStream).inputValueDefinition();
    }

    public String getStringValue(TerminalNode stringValue) {
        return stringValue.getText().substring(1, stringValue.getText().length() - 1);
    }

    public GraphqlParser getGraphqlParser(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return getGraphqlParser(charStream);
    }

    public GraphqlLexer getGraphqlLexer(String graphql) {
        CodePointCharStream charStream;
        charStream = CharStreams.fromString(graphql);
        return getGraphqlLexer(charStream);
    }

    private GraphqlLexer getGraphqlLexer(CharStream charStream) {
        GraphqlLexer lexer = new GraphqlLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                Logger.error(e);
                throw new GraphQLErrors(SYNTAX_ERROR.bind(msg, line, charPositionInLine), line, charPositionInLine);
            }
        });
        return lexer;
    }

    private GraphqlParser getGraphqlParser(CharStream charStream) {
        CommonTokenStream tokens = new CommonTokenStream(getGraphqlLexer(charStream));
        GraphqlParser parser = new GraphqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                Logger.error(e);
                throw new GraphQLErrors(SYNTAX_ERROR.bind(msg, line, charPositionInLine), line, charPositionInLine);
            }
        });
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        return parser;
    }
}
