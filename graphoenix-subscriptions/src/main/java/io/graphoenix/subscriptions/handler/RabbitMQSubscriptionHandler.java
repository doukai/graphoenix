package io.graphoenix.subscriptions.handler;

import com.rabbitmq.client.Delivery;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.handler.GraphQLVariablesProcessor;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import io.graphoenix.spi.handler.SubscriptionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.io.StringReader;
import java.util.Map;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static reactor.rabbitmq.BindingSpecification.binding;
import static reactor.rabbitmq.QueueSpecification.queue;

@ApplicationScoped
public class RabbitMQSubscriptionHandler implements SubscriptionHandler {

    private static final String SUBSCRIPTION_EXCHANGE_NAME = "subscription-exchange";

    private final IGraphQLDocumentManager manager;

    private final GraphQLVariablesProcessor variablesProcessor;

    private final JsonProvider jsonProvider;

    private final Provider<Mono<SubscriptionDataListener>> subscriptionDataListenerProvider;

    private final QueryHandler queryHandler;

    private final Sender sender;

    private final Receiver receiver;

    @Inject
    public RabbitMQSubscriptionHandler(IGraphQLDocumentManager manager, GraphQLVariablesProcessor variablesProcessor, JsonProvider jsonProvider, Provider<Mono<SubscriptionDataListener>> subscriptionDataListenerProvider, QueryHandler queryHandler, Sender sender, Receiver receiver) {
        this.manager = manager;
        this.variablesProcessor = variablesProcessor;
        this.jsonProvider = jsonProvider;
        this.subscriptionDataListenerProvider = subscriptionDataListenerProvider;
        this.queryHandler = queryHandler;
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public Flux<JsonValue> subscription(String graphQL, Map<String, JsonValue> variables) {
        return null;
    }

    @Override
    public Flux<JsonValue> subscription(OperationHandler operationHandler, String graphQL, Map<String, JsonValue> variables) {
        manager.registerFragment(graphQL);
        GraphqlParser.OperationDefinitionContext operationDefinitionContext = variablesProcessor.buildVariables(graphQL, variables);

        Stream<String> typeNameString = operationDefinitionContext.selectionSet().selection().stream()
                .flatMap(selectionContext ->
                        manager.getSubscriptionOperationTypeName()
                                .flatMap(name -> manager.getField(name, selectionContext.field().name().getText()))
                                .stream()
                )
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .distinct();

        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(REQUEST_ID)))
                .map(requestId -> (String) requestId)
                .flatMapMany(requestId ->
                        sender.declare(queue(requestId))
                                .thenMany(
                                        Flux.fromStream(typeNameString)
                                                .flatMap(typeName -> sender.bind(binding(SUBSCRIPTION_EXCHANGE_NAME, typeName, requestId)))
                                )
                                .flatMap(bindOk ->
                                        Flux.concat(
                                                queryHandler.query(operationHandler, graphQL, variables),
                                                receiver.consumeAutoAck(requestId).map(this::toJsonValue)
                                        )
                                )
                                .flatMap(jsonValue ->
                                        subscriptionDataListenerProvider.get()
                                                .flatMap(subscriptionDataListener -> subscriptionDataListener.merge(operationDefinitionContext, jsonValue))
                                )
                );
    }

    private JsonValue toJsonValue(Delivery delivery) {
        return jsonProvider.createReader(new StringReader(new String(delivery.getBody()))).readValue();
    }
}
