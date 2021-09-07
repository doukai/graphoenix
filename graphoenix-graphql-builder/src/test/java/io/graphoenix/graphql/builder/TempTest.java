package io.graphoenix.graphql.builder;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.introspection.IntrospectionBuilder;
import io.graphoenix.graphql.builder.introspection.IntrospectionDtoWrapper;
import io.graphoenix.graphql.builder.introspection.dto.__Schema;
import io.graphoenix.graphql.builder.schema.GraphqlSchemaBuilder;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

public class TempTest {

    @Test
    void test() throws IOException, URISyntaxException {
        URL url = Resources.getResource("test.graphqls");
        InputStream inputStream = url.openStream();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(inputStream);
        inputStream.close();

        StringWriter stringWriter = new StringWriter();

        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(graphqlAntlrManager);

        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);

        stringWriter.flush();

        graphqlAntlrManager.registerDocument(stringWriter.toString());


        stringWriter = new StringWriter();

        IntrospectionBuilder introspectionBuilder = new IntrospectionBuilder(graphqlAntlrManager);

        introspectionBuilder.buildObjectExpressions(stringWriter);

        stringWriter.flush();

        System.out.println(stringWriter);


    }
}
