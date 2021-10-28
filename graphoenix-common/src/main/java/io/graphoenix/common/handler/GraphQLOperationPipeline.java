package io.graphoenix.common.handler;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.common.config.HandlerFactory;
import io.graphoenix.spi.handler.OperationType;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.GraphQLResult;
import io.graphoenix.spi.handler.IGraphQLOperationHandler;
import io.graphoenix.spi.handler.IGraphQLOperationPipeline;
import io.graphoenix.spi.task.GraphQLTaskType;
import io.graphoenix.spi.task.IGraphQLTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AutoFactory
public class GraphQLOperationPipeline implements IGraphQLOperationPipeline<GraphQLRequestBody, GraphQLResult> {

    private final IGraphqlDocumentManager graphqlDocumentManager;
    private final List<IGraphQLOperationHandler<Object, Object>> handlerList;
    private final List<IGraphQLTask<Object, Object>> taskList;

    public GraphQLOperationPipeline(@Provided IGraphqlDocumentManager manager) {
        this.graphqlDocumentManager = manager;
        this.handlerList = new ArrayList<>();
        this.taskList = new ArrayList<>();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <H extends IGraphQLOperationHandler> GraphQLOperationPipeline push(Class<H> handlerClass) {
        IGraphQLOperationHandler<Object, Object> handler = HandlerFactory.HANDLER_FACTORY.create(handlerClass);
        handler.assign(this.graphqlDocumentManager);
        handlerList.add(handler);
        return this;
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <H extends IGraphQLTask> GraphQLOperationPipeline task(Class<H> taskClass, Object input) {
        IGraphQLTask<Object, Object> task = HandlerFactory.HANDLER_FACTORY.create(taskClass);
        task.init(input, GraphQLTaskType.BLOCK);
        task.assign(this.graphqlDocumentManager);
        taskList.add(task);
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <H extends IGraphQLTask> GraphQLOperationPipeline task(Class<H> taskClass) {
        IGraphQLTask<Object, Object> task = HandlerFactory.HANDLER_FACTORY.create(taskClass);
        task.init(GraphQLTaskType.BLOCK);
        task.assign(this.graphqlDocumentManager);
        taskList.add(task);
        return this;
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <H extends IGraphQLTask> GraphQLOperationPipeline asyncTask(Class<H> taskClass, Object input) {
        IGraphQLTask<Object, Object> task = HandlerFactory.HANDLER_FACTORY.create(taskClass);
        task.init(input, GraphQLTaskType.ASYNC);
        task.assign(this.graphqlDocumentManager);
        taskList.add(task);
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <H extends IGraphQLTask> GraphQLOperationPipeline asyncTask(Class<H> taskClass) {
        IGraphQLTask<Object, Object> task = HandlerFactory.HANDLER_FACTORY.create(taskClass);
        task.init(GraphQLTaskType.ASYNC);
        task.assign(this.graphqlDocumentManager);
        taskList.add(task);
        return this;
    }

    @Override
    public IGraphQLOperationPipeline<GraphQLRequestBody, GraphQLResult> build() {
        return this;
    }

    @Override
    public IGraphqlDocumentManager getManager() {
        return this.graphqlDocumentManager;
    }

    @Override
    public OperationType getOperationType(GraphQLRequestBody request) {
        GraphqlParser.OperationTypeContext operationTypeContext = graphqlDocumentManager.getOperationType(request.getQuery());
        if (operationTypeContext.QUERY() != null) {
            return OperationType.QUERY;
        } else if (operationTypeContext.MUTATION() != null) {
            return OperationType.MUTATION;
        } else if (operationTypeContext.SUBSCRIPTION() != null) {
            return OperationType.SUBSCRIPTION;
        }
        return null;
    }

    @Override
    public GraphQLResult order(GraphQLRequestBody request) {
        assert this.handlerList.size() > 0;
        OperationType type = this.getOperationType(request);
        Object result = request;
        for (IGraphQLOperationHandler<Object, Object> handler : this.handlerList) {
            switch (type) {
                case QUERY:
                    result = handler.query(result);
                    break;
                case MUTATION:
                    result = handler.mutation(result);
                    break;
                case SUBSCRIPTION:
                    result = handler.subscription(result);
                    break;
            }
        }
        assert result instanceof GraphQLResult;
        return (GraphQLResult) result;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public GraphQLOperationPipeline runTask() {
        this.taskList.parallelStream().filter(graphQLTask -> graphQLTask.getType().equals(GraphQLTaskType.ASYNC)).forEach(graphQLTask -> {
            new Thread(() -> {
                try {
                    graphQLTask.process();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        Object result = null;
        for (IGraphQLTask graphQLTask : this.taskList.stream().filter(graphQLTask -> graphQLTask.getType().equals(GraphQLTaskType.BLOCK)).collect(Collectors.toList())) {
            try {
                if (result != null) {
                    graphQLTask.init(result);
                }
                result = graphQLTask.process();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this;
    }
}
