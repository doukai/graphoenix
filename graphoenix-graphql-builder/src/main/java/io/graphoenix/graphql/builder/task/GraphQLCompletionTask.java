package io.graphoenix.graphql.builder.task;

import com.google.auto.service.AutoService;
import io.graphoenix.graphql.builder.schema.GraphqlSchemaRegister;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLCompletionTask;

import java.io.IOException;

@AutoService(IGraphQLCompletionTask.class)
public class GraphQLCompletionTask implements IGraphQLCompletionTask {

    private GraphQLTaskType type;

    private GraphqlSchemaRegister graphqlSchemaRegister;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(Void input) {

    }

    @Override
    public void init(Void input, GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void init(GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void assign(IGraphqlDocumentManager manager) {
        this.graphqlSchemaRegister = new GraphqlSchemaRegister(manager);
    }

    @Override
    public Void process() throws IOException {
        this.graphqlSchemaRegister.register();
        return null;
    }
}
