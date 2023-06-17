package io.graphoenix.subscriptions.handler;

import com.rabbitmq.client.Delivery;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationSubscriber;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.io.StringReader;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static reactor.rabbitmq.BindingSpecification.binding;
import static reactor.rabbitmq.QueueSpecification.queue;

@ApplicationScoped
public class RabbitMQOperationSubscriber implements OperationSubscriber {

    private static final String SUBSCRIPTION_EXCHANGE_NAME = "subscription-exchange";

    private final IGraphQLDocumentManager manager;

    private final JsonProvider jsonProvider;

    private final Provider<Mono<SubscriptionDataListener>> subscriptionDataListenerProvider;

    private final Sender sender;

    private final Receiver receiver;

    @Inject
    public RabbitMQOperationSubscriber(IGraphQLDocumentManager manager, JsonProvider jsonProvider, Provider<Mono<SubscriptionDataListener>> subscriptionDataListenerProvider, Sender sender, Receiver receiver) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
        this.subscriptionDataListenerProvider = subscriptionDataListenerProvider;
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public Flux<JsonValue> subscriptionOperation(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Mono<JsonValue> jsonValueMono) {

        Stream<String> typeNameStream = operationDefinitionContext.selectionSet().selection().stream()
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
                                        Flux.fromStream(typeNameStream)
                                                .flatMap(typeName -> sender.bind(binding(SUBSCRIPTION_EXCHANGE_NAME, typeName, requestId)))
                                )
                                .flatMap(bindOk ->
                                        Flux.concat(
                                                jsonValueMono,
                                                receiver.consumeAutoAck(requestId).map(this::toJsonValue)
                                        )
                                )
                                .flatMap(jsonValue ->
                                        subscriptionDataListenerProvider.get()
                                                .flatMap(subscriptionDataListener -> subscriptionDataListener.merge(operationDefinitionContext, jsonValue))
                                )
                );
    }

    @Override
    public Mono<Void> sendMutation(String typeName, JsonValue jsonValue) {
        JsonObjectBuilder mutation = jsonProvider.createObjectBuilder().add("type", typeName);
        if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            mutation.add("mutation", jsonProvider.createArrayBuilder(jsonValue.asJsonArray()));
        } else {
            mutation.add("mutation", jsonProvider.createArrayBuilder().add(jsonProvider.createObjectBuilder(jsonValue.asJsonObject())));
        }
        return sender.send(
                Mono.just(
                        new OutboundMessage(
                                SUBSCRIPTION_EXCHANGE_NAME,
                                typeName,
                                mutation.build().toString().getBytes()
                        )
                )
        );
    }

    private JsonValue toJsonValue(Delivery delivery) {
        return jsonProvider.createReader(new StringReader(new String(delivery.getBody()))).readValue();
    }
}
