package io.graphoenix.greator;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.grantlr.manager.impl.GraphqlAntlrManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TypeExpressionInputBuilder {

    private final GraphqlAntlrManager manager;

    public TypeExpressionInputBuilder(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public void buildObjectExpressionInputs() throws IOException {

        List<GraphqlObject> objects = manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::objectTypeDefinitionToDto).collect(Collectors.toList());

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("typeExpression.mustache");
        mustache.execute(new PrintWriter(this.getClass().getResource("/").getPath() + "/aaa.txt"), Map.of("objects", objects)).flush();
    }

    private GraphqlObject objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new GraphqlObject(objectTypeDefinitionContext.name().getText(),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList()));
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new GraphqlField(fieldDefinitionContext.name().getText(), manager.getFieldTypeName(fieldDefinitionContext.type()));
    }
}
