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

        URL url = Resources.getResource("introspection.gql");
        InputStream inputStream = url.openStream();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(inputStream);
        inputStream.close();

        url =Resources.getResource("auth.gql");
        inputStream = url.openStream();
        graphqlAntlrManager.registerDocument(inputStream);
        inputStream.close();

        StringWriter stringWriter = new StringWriter();
        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(graphqlAntlrManager);
        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);

        graphqlAntlrManager.registerDocument(stringWriter.toString());
        stringWriter.close();

        stringWriter = new StringWriter();

        IntrospectionBuilder introspectionBuilder = new IntrospectionBuilder(graphqlAntlrManager);

        introspectionBuilder.buildObjectExpressions(stringWriter);

        System.out.println(stringWriter);
        stringWriter.close();


    }

    @Test
    void testDtoWrapper() throws IOException {

        URL url = Resources.getResource("auth.gql");
        InputStream inputStream = url.openStream();
        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(inputStream);
        inputStream.close();

        StringWriter stringWriter = new StringWriter();

        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(graphqlAntlrManager);

        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);

        stringWriter.flush();

        graphqlAntlrManager.registerDocument(stringWriter.toString());
//        System.out.println(stringWriter);

        IntrospectionDtoWrapper wrapper = new IntrospectionDtoWrapper(graphqlAntlrManager);
        __Schema schema = wrapper.buildIntrospectionSchema();
        Gson gson = new Gson();
        System.out.println(gson.toJson(schema));
    }
}
