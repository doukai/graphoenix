package io.graphoenix.graphql.builder.task;

import com.google.auto.service.AutoService;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTypeFileRegisterTask;

import java.io.IOException;
import java.io.InputStream;

@AutoService(IGraphQLTypeFileRegisterTask.class)
public class GraphQLTypeFileRegisterTask implements IGraphQLTypeFileRegisterTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private String fileName;

    private GraphQLTaskType type;

    @Override
    public GraphQLTaskType getType() {
        return this.type;
    }

    @Override
    public void init(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void init(String fileName, GraphQLTaskType type) {
        this.fileName = fileName;
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
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(this.fileName);
        this.graphqlDocumentManager.registerDocument(inputStream);
        return null;
    }
}
