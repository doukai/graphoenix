package io.graphoenix.subscriptions.handler;

import com.rabbitmq.client.Delivery;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.PublisherBeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLVariablesProcessor;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.core.handler.OperationSubscriber;
import io.graphoenix.core.handler.SubscriptionDataListener;
import io.graphoenix.spi.handler.SubscriptionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static reactor.rabbitmq.BindingSpecification.binding;
import static reactor.rabbitmq.QueueSpecification.queue;

@ApplicationScoped
public class RabbitMQOperationSubscriber extends OperationSubscriber {

    public static final String SUBSCRIPTION_EXCHANGE_NAME = "graphoenix.subscription";

    private final GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    private final GraphQLVariablesProcessor variablesProcessor;

    private final SubscriptionHandler subscriptionHandler;

    private final JsonProvider jsonProvider;

    private final Provider<SubscriptionDataListener> subscriptionDataListenerProvider;

    private final Sender sender;

    private final Receiver receiver;

    @Inject
    public RabbitMQOperationSubscriber(GraphQLConfig graphQLConfig, IGraphQLDocumentManager manager, GraphQLVariablesProcessor variablesProcessor, SubscriptionHandler subscriptionHandler, JsonProvider jsonProvider, Provider<SubscriptionDataListener> subscriptionDataListenerProvider, Sender sender, Receiver receiver) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
        this.variablesProcessor = variablesProcessor;
        this.subscriptionHandler = subscriptionHandler;
        this.jsonProvider = jsonProvider;
        this.subscriptionDataListenerProvider = subscriptionDataListenerProvider;
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public Flux<JsonValue> subscriptionOperation(OperationHandler operationHandler, String graphQL, Map<String, JsonValue> variables, String token, String operationId) {
        manager.registerFragment(graphQL);
        GraphqlParser.OperationDefinitionContext operationDefinitionContext = variablesProcessor.buildVariables(graphQL, variables);

        registerSelection(operationDefinitionContext);

        Stream<String> typeNameStream = operationDefinitionContext.selectionSet().selection().stream()
                .flatMap(selectionContext ->
                        manager.getSubscriptionOperationTypeName()
                                .flatMap(name -> manager.getField(name, selectionContext.field().name().getText()))
                                .stream()
                )
                .map(fieldDefinitionContext -> manager.getPackageName(fieldDefinitionContext.type()).orElse(graphQLConfig.getPackageName()) + "." + manager.getFieldTypeName(fieldDefinitionContext.type()))
                .distinct();

        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(REQUEST_ID)))
                .map(requestId -> (String) requestId)
                .flatMapMany(requestId ->
                        sender.declare(queue(requestId).autoDelete(true))
                                .thenMany(
                                        Flux.fromStream(typeNameStream)
                                                .flatMap(typeName -> sender.bind(binding(SUBSCRIPTION_EXCHANGE_NAME, typeName, requestId)))
                                )
                                .flatMap(bindOk ->
                                        PublisherBeanContext.getMono(SubscriptionDataListener.class)
                                                .map(subscriptionDataListener -> subscriptionDataListener.indexFilter(operationDefinitionContext))
                                                .flatMapMany(subscriptionDataListener ->
                                                        Flux.concat(
                                                                subscriptionHandler.subscription(operationHandler, operationDefinitionContext),
                                                                receiver.consumeAutoAck(requestId)
                                                                        .map(this::toJsonValue)
                                                                        .filter(subscriptionDataListener::merged)
                                                                        .flatMap(jsonValue -> subscriptionHandler.subscription(operationHandler, operationDefinitionContext))
                                                        )
                                                                .doOnNext(jsonValue -> subscriptionDataListener.indexData(operationDefinitionContext, jsonValue))
                                                                .flatMap(jsonValue -> subscriptionHandler.invoke(operationDefinitionContext, jsonValue))
                                                )
                                )
                )
                .contextWrite(PublisherBeanContext.of(SubscriptionDataListener.class, subscriptionDataListenerProvider.get()));
    }

    @Override
    public Mono<JsonValue> sendMutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Operation operation, JsonValue jsonValue) {
        Flux<OutboundMessage> messageFlux = Flux.fromIterable(operationDefinitionContext.selectionSet().selection())
                .map(selectionContext -> {
                            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getMutationOperationTypeName()
                                    .map(name ->
                                            manager.getField(name, selectionContext.field().name().getText())
                                                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(name, selectionContext.field().name().getText())))
                                    )
                                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
                            String packageName = manager.getPackageName(fieldDefinitionContext.type()).orElse(graphQLConfig.getPackageName());
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            JsonObjectBuilder mutation = jsonProvider.createObjectBuilder().add("type", fieldTypeName);
                            JsonObject arguments = operation.getField(selectionContext.field().name().getText()).getArguments();
                            String selectionName = Optional.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText());
                            JsonValue selectionJsonValue = jsonValue.asJsonObject().get(selectionName);
                            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                mutation.add("arguments", jsonProvider.createArrayBuilder(arguments.get(LIST_INPUT_NAME).asJsonArray()))
                                        .add("mutation", jsonProvider.createArrayBuilder(selectionJsonValue.asJsonArray()));
                            } else {
                                mutation.add("arguments", jsonProvider.createArrayBuilder().add(jsonProvider.createObjectBuilder(arguments.asJsonObject())))
                                        .add("mutation", jsonProvider.createArrayBuilder().add(jsonProvider.createObjectBuilder(selectionJsonValue.asJsonObject())));
                            }
                            return new OutboundMessage(
                                    SUBSCRIPTION_EXCHANGE_NAME,
                                    packageName + "." + fieldTypeName,
                                    mutation.build().toString().getBytes()
                            );
                        }
                );

        return sender.send(messageFlux).thenReturn(jsonValue);
    }

    private JsonValue toJsonValue(Delivery delivery) {
        return jsonProvider.createReader(new StringReader(new String(delivery.getBody()))).readValue();
    }
}
