package io.graphoenix.graphql.builder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;

public class TempTest {

    @Test
    void test() throws IOException {

        IGraphQLDocumentManager graphqlAntlrManager = new GraphQLDocumentManager(
                new GraphQLOperationManager(),
                new GraphQLSchemaManager(),
                new GraphQLDirectiveManager(),
                new GraphQLObjectManager(),
                new GraphQLInterfaceManager(),
                new GraphQLUnionManager(),
                new GraphQLFieldManager(),
                new GraphQLInputObjectManager(),
                new GraphQLInputValueManager(),
                new GraphQLEnumManager(),
                new GraphQLScalarManager(),
                new GraphQLFragmentManager()
        );


        URL url = Resources.getResource("auth.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);
        graphqlAntlrManager.registerDocument(graphql);

        DocumentBuilder documentBuilder = new DocumentBuilder(graphqlAntlrManager);
        System.out.println(documentBuilder.buildDocument().toString());

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
