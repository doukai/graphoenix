package io.graphoenix.graphql.builder;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.dto.GraphqlDtoWrapper;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class EnumExpressionBuilder {

    private final GraphqlDtoWrapper wrapper;

    public EnumExpressionBuilder(GraphqlAntlrManager manager) {
        this.wrapper = new GraphqlDtoWrapper(manager);
    }

    public void buildObjectExpressions(Writer writer) throws IOException {

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("enumExpression.mustache");
        mustache.execute(writer, Map.of("enums", wrapper.enumTypeDefinitionsToDto())).flush();
    }
}


