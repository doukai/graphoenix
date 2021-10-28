package io.graphoenix.graphql.builder.task;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTypeInputStreamRegisterTask;

import java.io.IOException;
import java.io.InputStream;

@AutoService(IGraphQLTypeInputStreamRegisterTask.class)
public class GraphQLTypeInputStreamRegisterTask implements IGraphQLTypeInputStreamRegisterTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private InputStream inputStream;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void init(InputStream inputStream, GraphQLTaskType type) {
        this.inputStream = inputStream;
        this.type = type;
    }

    @Override
    public void init(GraphQLTaskType type) {
        this.type = type;
    }

    @Override
    public void assign(IGraphqlDocumentManager manager) {
        this.graphqlDocumentManager = manager;
    }

    @Override
    public Void process() throws IOException {
        this.graphqlDocumentManager.registerDocument(this.inputStream);
        inputStream.close();
        return null;
    }
}
