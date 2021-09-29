package io.graphoenix.graphql.builder.schema;

import com.google.common.io.Resources;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

public class GraphqlSchemaRegister {

    private final GraphqlAntlrManager manager;

    public GraphqlSchemaRegister(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public void register() throws IOException {
        URL url = Resources.getResource("graphql/introspectionTypes.gql");
        InputStream inputStream = url.openStream();
        manager.registerDocument(inputStream);
        inputStream.close();
        StringWriter stringWriter = new StringWriter();
        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(manager);
        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);
        System.out.println(stringWriter);
        manager.registerDocument(stringWriter.toString());
        stringWriter.close();
    }
}
