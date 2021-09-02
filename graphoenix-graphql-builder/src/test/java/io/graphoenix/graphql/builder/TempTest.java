package io.graphoenix.graphql.builder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

public class TempTest {

    @Test
    void test() throws IOException {
        URL url = Resources.getResource("test.graphqls");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        TypeExpressionBuilder typeExpressionBuilder = new TypeExpressionBuilder(graphqlAntlrManager);
    }
}
