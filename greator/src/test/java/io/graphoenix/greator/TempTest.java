package io.graphoenix.greator;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.grantlr.manager.impl.GraphqlAntlrManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

public class TempTest {

    @Test
    void test() throws IOException {
        URL url = Resources.getResource("test.graphqls");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        TypeExpressionInputBuilder typeExpressionInputBuilder = new TypeExpressionInputBuilder(graphqlAntlrManager);
        typeExpressionInputBuilder.buildObjectExpressionInputs();
    }
}
