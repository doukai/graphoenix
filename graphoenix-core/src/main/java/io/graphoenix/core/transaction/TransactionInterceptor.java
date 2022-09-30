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
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_ID;

@ApplicationScoped
@Transactional
@Priority(0)
@Interceptor
public class TransactionInterceptor {

    private final Provider<Mono<Connection>> connectionProvider;

    @Inject
    public TransactionInterceptor(Provider<Mono<Connection>> connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Publisher<?> aroundInvoke(InvocationContext invocationContext) throws NoSuchMethodException {
        Transactional.TxType txType;
        Class<? extends Exception>[] rollbackOn;
        Class<? extends Exception>[] dontRollbackOn;

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

        try {
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
                            return Mono.from(connection.rollbackTransaction()).thenEmpty(connection.close()).thenEmpty(Mono.error(throwable));
                        },
                        connection -> Mono.from(connection.rollbackTransaction()).thenEmpty(connection.close())
                ).contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                Mono<?> nonTransaction = connectionProvider.get()
                        .map(connection -> connection.setAutoCommit(true))
                        .then(CheckedFunction0.of(() -> (Mono<Object>) invocationContext.proceed()).unchecked().get())
                        .contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));

                switch (txType) {
                    case REQUIRED:
                        return connectionProvider.get().then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(newTransaction);
                    case REQUIRES_NEW:
                        return newTransaction;
                    case MANDATORY:
                        return connectionProvider.get().then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(Mono.error(new TransactionRequiredException()));
                    case SUPPORTS:
                        return nonTransaction;
                    case NOT_SUPPORTED:
                        return connectionProvider.get().then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(nonTransaction);
                    case NEVER:
                        return connectionProvider.get().then(Mono.error(new InvalidTransactionException())).switchIfEmpty(nonTransaction);

                }
                return connectionProvider.get().then((Mono<Object>) invocationContext.proceed()).switchIfEmpty(newTransaction);

            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(Flux.class)) {
                return ((Flux<?>) invocationContext.proceed())
                        .thenMany(connectionProvider.get().map(Connection::commitTransaction))
                        .contextWrite(Context.of(TRANSACTION_ID, NanoIdUtils.randomNanoId()));
            } else if (invocationContext.getMethod().getReturnType().isAssignableFrom(PublisherBuilder.class)) {

            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }


        switch (txType) {
            case REQUIRED:
            case REQUIRES_NEW:
            case MANDATORY:
            case SUPPORTS:
            case NOT_SUPPORTED:
            case NEVER:
        }

        return null;
    }
}
