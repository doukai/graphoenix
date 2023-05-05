package io.graphoenix.r2dbc.connector.transaction;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.transaction.TransactionInterceptorProcessor;
import io.r2dbc.spi.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.reactivestreams.Publisher;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Arrays;

import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_ID;
import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.TransactionType.IN_TRANSACTION;
import static io.graphoenix.spi.constant.Hammurabi.TransactionType.NO_TRANSACTION;

@ApplicationScoped
public class R2dbcTransactionInterceptorProcessor implements TransactionInterceptorProcessor {

    private final Provider<Mono<Connection>> connectionProvider;
    private final ReactiveStreamsFactory reactiveStreamsFactory;

    @Inject
    public R2dbcTransactionInterceptorProcessor(Provider<Mono<Connection>> connectionProvider, ReactiveStreamsFactory reactiveStreamsFactory) {
        this.connectionProvider = connectionProvider;
        this.reactiveStreamsFactory = reactiveStreamsFactory;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Object aroundInvoke(InvocationContext invocationContext) {
        Transactional.TxType txType;
        Class<? extends Exception>[] rollbackOn;
        Class<? extends Exception>[] dontRollbackOn;

        try {
            if (invocationContext.getContextData().containsKey("value")) {
                txType = (Transactional.TxType) invocationContext.getContextData().get("value");
            } else {
                txType = (Transactional.TxType) Transactional.class.getDeclaredMethod("value").getDefaultValue();
            }
            if (invocationContext.getContextData().containsKey("rollbackOn")) {
                rollbackOn = (Class<? extends Exception>[]) invocationContext.getContextData().get("rollbackOn");
            } else {
                rollbackOn = (Class<? extends Exception>[]) Transactional.class.getDeclaredMethod("rollbackOn").getDefaultValue();
            }
            if (invocationContext.getContextData().containsKey("dontRollbackOn")) {
                dontRollbackOn = (Class<? extends Exception>[]) invocationContext.getContextData().get("dontRollbackOn");
            } else {
                dontRollbackOn = (Class<? extends Exception>[]) Transactional.class.getDeclaredMethod("dontRollbackOn").getDefaultValue();
            }

            if (invocationContext.getMethod().getReturnType().isAssignableFrom(Mono.class)) {
                switch (txType) {
                    case REQUIRED:
                        return Mono.empty()
                                .transformDeferredContextual(
                                        (mono, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Mono<Object>) invocationContext.proceed() :
                                                        Mono.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Mono.from(connection.setAutoCommit(false))
                                                                                .then(Mono.from(connection.beginTransaction()))
                                                                                .then((Mono<Object>) invocationContext.proceed())
                                                                                .doOnError(throwable -> Mono.error(throwable));
                                                                    } catch (Exception e) {
                                                                        return Mono.error(e);
                                                                    }
                                                                },
                                                                connection -> Mono.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                                                (connection, throwable) -> {
                                                                    Logger.error(throwable);
                                                                    return Mono.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Mono.error(throwable));
                                                                },
                                                                connection -> Mono.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                                            } catch (Exception e) {
                                                return Mono.error(e);
                                            }
                                        }
                                );
                    case REQUIRES_NEW:
                        return Mono.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Mono.from(connection.setAutoCommit(false))
                                                .then(Mono.from(connection.beginTransaction()))
                                                .then((Mono<Object>) invocationContext.proceed());
                                    } catch (Exception e) {
                                        return Mono.error(e);
                                    }
                                },
                                connection -> Mono.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                (connection, throwable) -> {
                                    Logger.error(throwable);
                                    return Mono.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Mono.error(throwable));
                                },
                                connection -> Mono.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                    case MANDATORY:
                        return Mono.empty()
                                .transformDeferredContextual(
                                        (mono, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Mono<Object>) invocationContext.proceed() :
                                                        Mono.error(new TransactionRequiredException());
                                            } catch (Exception e) {
                                                return Mono.error(e);
                                            }
                                        }
                                );
                    case SUPPORTS:
                        return Mono.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Mono.from(connection.setAutoCommit(true)).then((Mono<Object>) invocationContext.proceed());
                                    } catch (Exception e) {
                                        return Mono.error(e);
                                    }
                                },
                                Connection::close
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                    case NOT_SUPPORTED:
                        return Mono.empty()
                                .transformDeferredContextual(
                                        (mono, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Mono<Object>) invocationContext.proceed() :
                                                        Mono.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Mono.from(connection.setAutoCommit(true)).then((Mono<Object>) invocationContext.proceed());
                                                                    } catch (Exception e) {
                                                                        return Mono.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                                            } catch (Exception e) {
                                                return Mono.error(e);
                                            }
                                        }
                                );
                    case NEVER:
                        return Mono.empty()
                                .transformDeferredContextual(
                                        (mono, contextView) ->
                                                contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        Mono.error(new InvalidTransactionException()) :
                                                        Mono.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Mono.from(connection.setAutoCommit(true)).then((Mono<Object>) invocationContext.proceed());
                                                                    } catch (Exception e) {
                                                                        return Mono.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION))
                                );
                    default:
                        throw new NotSupportedException();
                }
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(Flux.class)) {
                switch (txType) {
                    case REQUIRED:
                        return Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Flux<Object>) invocationContext.proceed() :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection ->
                                                                {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(false))
                                                                                .thenMany(Flux.from(connection.beginTransaction()))
                                                                                .thenMany((Flux<Object>) invocationContext.proceed());
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                                                (connection, throwable) -> {
                                                                    Logger.error(throwable);
                                                                    return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                                                                },
                                                                connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                    case REQUIRES_NEW:
                        return Flux.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Flux.from(connection.setAutoCommit(false))
                                                .thenMany(Flux.from(connection.beginTransaction()))
                                                .thenMany((Flux<Object>) invocationContext.proceed());
                                    } catch (Exception e) {
                                        return Flux.error(e);
                                    }
                                },
                                connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                (connection, throwable) -> {
                                    Logger.error(throwable);
                                    return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                                },
                                connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                    case MANDATORY:
                        return Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Flux<Object>) invocationContext.proceed() :
                                                        Flux.error(new TransactionRequiredException());
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                    case SUPPORTS:
                        return Flux.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Flux.from(connection.setAutoCommit(true)).thenMany((Flux<Object>) invocationContext.proceed());
                                    } catch (Exception e) {
                                        return Flux.error(e);
                                    }
                                },
                                Connection::close
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                    case NOT_SUPPORTED:
                        return Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        (Flux<Object>) invocationContext.proceed() :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(true)).thenMany((Flux<Object>) invocationContext.proceed());
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                    case NEVER:
                        return Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) ->
                                                contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        Flux.error(new InvalidTransactionException()) :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(true)).thenMany((Flux<Object>) invocationContext.proceed());
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION))
                                );
                    default:
                        throw new NotSupportedException();
                }
            } else {
                Flux<Object> proceed;
                if (invocationContext.getMethod().getReturnType().isAssignableFrom(Publisher.class)) {
                    proceed = Flux.from((Publisher<Object>) invocationContext.proceed());
                } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(PublisherBuilder.class)) {
                    proceed = Flux.from(((PublisherBuilder<Object>) invocationContext.proceed()).buildRs());
                } else {
                    throw new NotSupportedException();
                }
                Flux<Object> transaction;
                switch (txType) {
                    case REQUIRED:
                        transaction = Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        proceed :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(false))
                                                                                .thenMany(Flux.from(connection.beginTransaction()))
                                                                                .thenMany(proceed);
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                                                (connection, throwable) -> {
                                                                    Logger.error(throwable);
                                                                    return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                                                                },
                                                                connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                        break;
                    case REQUIRES_NEW:
                        transaction = Flux.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Flux.from(connection.setAutoCommit(false))
                                                .thenMany(Flux.from(connection.beginTransaction()))
                                                .thenMany(proceed);
                                    } catch (Exception e) {
                                        return Flux.error(e);
                                    }
                                },
                                connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                                (connection, throwable) -> {
                                    Logger.error(throwable);
                                    return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                                },
                                connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, IN_TRANSACTION));
                        break;
                    case MANDATORY:
                        transaction = Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        proceed :
                                                        Flux.error(new TransactionRequiredException());
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                        break;
                    case SUPPORTS:
                        transaction = Flux.usingWhen(
                                connectionProvider.get(),
                                connection -> {
                                    try {
                                        return Flux.from(connection.setAutoCommit(true)).thenMany(proceed);
                                    } catch (Exception e) {
                                        return Flux.error(e);
                                    }
                                },
                                Connection::close
                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                        break;
                    case NOT_SUPPORTED:
                        transaction = Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) -> {
                                            try {
                                                return contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        proceed :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(true)).thenMany(proceed);
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION));
                                            } catch (Exception e) {
                                                return Flux.error(e);
                                            }
                                        }
                                );
                        break;
                    case NEVER:
                        transaction = Flux.empty()
                                .transformDeferredContextual(
                                        (flux, contextView) ->
                                                contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION) ?
                                                        Flux.error(new InvalidTransactionException()) :
                                                        Flux.usingWhen(
                                                                connectionProvider.get(),
                                                                connection -> {
                                                                    try {
                                                                        return Flux.from(connection.setAutoCommit(true)).thenMany(proceed);
                                                                    } catch (Exception e) {
                                                                        return Flux.error(e);
                                                                    }
                                                                },
                                                                Connection::close
                                                        ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId(), TRANSACTION_TYPE, NO_TRANSACTION))
                                );
                        break;
                    default:
                        throw new NotSupportedException();
                }
                if (invocationContext.getMethod().getReturnType().isAssignableFrom(Publisher.class)) {
                    return transaction;
                } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(PublisherBuilder.class)) {
                    return reactiveStreamsFactory.fromPublisher(transaction);
                } else {
                    throw new NotSupportedException();
                }
            }
        } catch (Exception exception) {
            if (invocationContext.getMethod().getReturnType().isAssignableFrom(Mono.class)) {
                return Mono.error(exception);
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(Flux.class)) {
                return Flux.error(exception);
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(Publisher.class)) {
                return Flux.error(exception);
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(PublisherBuilder.class)) {
                return reactiveStreamsFactory.fromPublisher(Flux.error(exception));
            } else {
                throw new RuntimeException(exception);
            }
        }
    }

    Publisher<Void> errorProcess(Connection connection, Throwable throwable, Class<? extends Exception>[] rollbackOn, Class<? extends Exception>[] dontRollbackOn) {
        if (rollbackOn != null && rollbackOn.length > 0) {
            if (Arrays.stream(rollbackOn).anyMatch(exception -> exception.equals(throwable.getClass()))) {
                return connection.rollbackTransaction();
            } else {
                return connection.commitTransaction();
            }
        } else if (dontRollbackOn != null && dontRollbackOn.length > 0) {
            if (Arrays.stream(dontRollbackOn).anyMatch(exception -> exception.equals(throwable.getClass()))) {
                return connection.commitTransaction();
            } else {
                return connection.rollbackTransaction();
            }
        }
        return connection.rollbackTransaction();
    }
}
