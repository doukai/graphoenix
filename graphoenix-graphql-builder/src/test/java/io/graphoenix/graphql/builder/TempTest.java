package io.graphoenix.graphql.builder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.introspection.IntrospectionBuilder;
import io.graphoenix.graphql.builder.introspection.IntrospectionDtoWrapper;
import io.graphoenix.graphql.builder.introspection.dto.__Schema;
import io.graphoenix.graphql.builder.schema.GraphQLDocumentBuilder;
import io.graphoenix.graphql.builder.schema.GraphqlSchemaBuilder;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

public class TempTest {

    @Test
    void test() throws IOException, URISyntaxException {

        IGraphqlDocumentManager graphqlAntlrManager = new GraphqlDocumentManager(
                new GraphqlOperationManager(),
                new GraphqlSchemaManager(),
                new GraphqlDirectiveManager(),
                new GraphqlObjectManager(),
                new GraphqlFieldManager(),
                new GraphqlInputObjectManager(),
                new GraphqlInputValueManager(),
                new GraphqlEnumManager(),
                new GraphqlScalarManager(),
                new GraphqlFragmentManager()
        );


        URL url = Resources.getResource("auth.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);
        graphqlAntlrManager.registerDocument(graphql);

        GraphQLDocumentBuilder graphQLDocumentBuilder = new GraphQLDocumentBuilder(graphqlAntlrManager);
        System.out.println(graphQLDocumentBuilder.buildDocument().toString());

    }

    @Test
    void testDtoWrapper() throws IOException {

//        URL url = Resources.getResource("auth.gql");
//        InputStream inputStream = url.openStream();
//        IGraphqlDocumentManager graphqlAntlrManager = new GraphqlAntlrManager(inputStream);
//        inputStream.close();
//
//        StringWriter stringWriter = new StringWriter();
//
//        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(graphqlAntlrManager);
//
//        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);
//
//        stringWriter.flush();
//
//        graphqlAntlrManager.registerDocument(stringWriter.toString());
////        System.out.println(stringWriter);
//
//        IntrospectionDtoWrapper wrapper = new IntrospectionDtoWrapper(graphqlAntlrManager);
//        __Schema schema = wrapper.buildIntrospectionSchema();
//        Gson gson = new Gson();
//        System.out.println(gson.toJson(schema));
    }
}
