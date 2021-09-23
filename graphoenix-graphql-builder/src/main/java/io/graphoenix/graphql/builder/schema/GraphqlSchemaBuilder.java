package io.graphoenix.graphql.builder.schema;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class GraphqlSchemaBuilder {

    private final GraphqlDtoWrapper wrapper;

    public GraphqlSchemaBuilder(GraphqlAntlrManager manager) {
        this.wrapper = new GraphqlDtoWrapper(manager);
    }

    public void buildObjectExpressions(Writer writer) throws IOException {

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("mustache/schema.mustache");
        mustache.execute(writer, Map.of(
                "objects", wrapper.objectTypeDefinitionsToDto(),
                "enums", wrapper.enumTypeDefinitionsToDto()
        )).flush();
    }
}


