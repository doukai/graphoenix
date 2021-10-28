package io.graphoenix.mysql.task;

import com.google.auto.service.AutoService;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLIntrospectionTypeRegisterTask;

import java.io.IOException;
import java.io.InputStream;

@AutoService(IGraphQLIntrospectionTypeRegisterTask.class)
public class GraphQLIntrospectionTypeRegisterTask implements IGraphQLIntrospectionTypeRegisterTask {

    private IGraphqlDocumentManager graphqlDocumentManager;

    private GraphQLTaskType type;

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
        this.graphqlDocumentManager = manager;
    }

    @Override
    public Void process() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Hammurabi.INTROSPECTION_TYPES_FILE_NAME);
        this.graphqlDocumentManager.registerDocument(inputStream);
        return null;
    }
}
