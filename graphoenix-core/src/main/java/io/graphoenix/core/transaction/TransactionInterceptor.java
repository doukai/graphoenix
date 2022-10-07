package io.graphoenix.core.transaction;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.r2dbc.spi.Connection;
import io.vavr.CheckedFunction0;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
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

@ApplicationScoped
@Transactional
@Priority(0)
@Interceptor
public class TransactionInterceptor {

    private final Provider<Mono<Connection>> connectionProvider;
    private final ReactiveStreamsFactory reactiveStreamsFactory;

    @Inject
    public TransactionInterceptor(Provider<Mono<Connection>> connectionProvider, ReactiveStreamsFactory reactiveStreamsFactory) {
        this.connectionProvider = connectionProvider;
        this.reactiveStreamsFactory = reactiveStreamsFactory;
    }

    @SuppressWarnings("unchecked")
    @AroundInvoke
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
            Mono<Connection> transactionConnection = connectionProvider.get().filter(connection -> !connection.isAutoCommit());

            if (invocationContext.getMethod().getReturnType().isAssignableFrom(Mono.class)) {
                Mono<?> newTransaction = Mono.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Mono.from(connection.setAutoCommit(false))
                                        .then(Mono.from(connection.beginTransaction()))
                                        .then(CheckedFunction0.of(() -> (Mono<Object>) invocationContext.proceed()).unchecked().get()),
                        connection -> Mono.from(connection.commitTransaction()).thenEmpty(connection.close()),
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            return Mono.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Mono.error(throwable));
                        },
                        connection -> Mono.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));


                Mono<?> nonTransaction = Mono.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Mono.from(connection.setAutoCommit(true))
                                        .then(CheckedFunction0.of(() -> (Mono<Object>) invocationContext.proceed()).unchecked().get()),
                        Connection::close
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                switch (txType) {
                    case REQUIRED:
                        return transactionConnection.then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(newTransaction);
                    case REQUIRES_NEW:
                        return newTransaction;
                    case MANDATORY:
                        return transactionConnection.then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(Mono.error(new TransactionRequiredException()));
                    case SUPPORTS:
                        return nonTransaction;
                    case NOT_SUPPORTED:
                        return transactionConnection.then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(nonTransaction);
                    case NEVER:
                        return transactionConnection.then(Mono.error(new InvalidTransactionException())).switchIfEmpty(nonTransaction);
                    default:
                        throw new NotSupportedException();
                }
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(Flux.class)) {
                Flux<?> newTransaction = Flux.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Flux.from(connection.setAutoCommit(false))
                                        .thenMany(Flux.from(connection.beginTransaction()))
                                        .thenMany(CheckedFunction0.of(() -> (Flux<Object>) invocationContext.proceed()).unchecked().get()),
                        connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                        },
                        connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                Flux<?> nonTransaction = Flux.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Flux.from(connection.setAutoCommit(true))
                                        .thenMany(CheckedFunction0.of(() -> (Flux<Object>) invocationContext.proceed()).unchecked().get()),
                        Connection::close
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                switch (txType) {
                    case REQUIRED:
                        return transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(newTransaction);
                    case REQUIRES_NEW:
                        return newTransaction;
                    case MANDATORY:
                        return transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(Flux.error(new TransactionRequiredException()));
                    case SUPPORTS:
                        return nonTransaction;
                    case NOT_SUPPORTED:
                        return transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(nonTransaction);
                    case NEVER:
                        return transactionConnection.thenMany(Flux.error(new InvalidTransactionException())).switchIfEmpty(nonTransaction);
                    default:
                        throw new NotSupportedException();
                }
            } else {
                Flux<?> proceed;
                if (invocationContext.getMethod().getReturnType().isAssignableFrom(Publisher.class)) {
                    proceed = Flux.from(((Publisher<?>) invocationContext.proceed()));
                } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(PublisherBuilder.class)) {
                    proceed = Flux.from(((PublisherBuilder<?>) invocationContext.proceed()).buildRs());
                } else {
                    throw new NotSupportedException();
                }
                Flux<?> newTransaction = Flux.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Flux.from(connection.setAutoCommit(false))
                                        .thenMany(Flux.from(connection.beginTransaction()))
                                        .thenMany(CheckedFunction0.of(() -> proceed).unchecked().get()),
                        connection -> Flux.from(connection.commitTransaction()).thenEmpty(connection.close()),
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            return Flux.from(errorProcess(connection, throwable, rollbackOn, dontRollbackOn)).thenEmpty(connection.close()).thenEmpty(Flux.error(throwable));
                        },
                        connection -> Flux.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                Flux<?> nonTransaction = Flux.usingWhen(
                        connectionProvider.get(),
                        connection ->
                                Flux.from(connection.setAutoCommit(true))
                                        .thenMany(CheckedFunction0.of(() -> proceed).unchecked().get()),
                        Connection::close
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                Flux<?> transaction;
                switch (txType) {
                    case REQUIRED:
                        transaction = transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(newTransaction);
                        break;
                    case REQUIRES_NEW:
                        transaction = newTransaction;
                        break;
                    case MANDATORY:
                        transaction = transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(Flux.error(new TransactionRequiredException()));
                        break;
                    case SUPPORTS:
                        transaction = nonTransaction;
                        break;
                    case NOT_SUPPORTED:
                        transaction = transactionConnection.thenMany((Flux<Object>) invocationContext.proceed()).switchIfEmpty(nonTransaction);
                        break;
                    case NEVER:
                        transaction = transactionConnection.thenMany(Flux.error(new InvalidTransactionException())).switchIfEmpty(nonTransaction);
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
