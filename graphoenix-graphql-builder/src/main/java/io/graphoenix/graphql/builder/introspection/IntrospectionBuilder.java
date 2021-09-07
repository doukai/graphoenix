package io.graphoenix.graphql.builder.introspection;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.introspection.dto.__Schema;

import java.io.IOException;
import java.io.Writer;

public class IntrospectionBuilder {

    private final IntrospectionDtoWrapper wrapper;

    public IntrospectionBuilder(GraphqlAntlrManager manager) {
        this.wrapper = new IntrospectionDtoWrapper(manager);
    }

    public void buildObjectExpressions(Writer writer) throws IOException {

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("introspection.mustache");
        __Schema schema = wrapper.buildIntrospectionSchema();
        mustache.execute(writer, schema).flush();
    }
}


