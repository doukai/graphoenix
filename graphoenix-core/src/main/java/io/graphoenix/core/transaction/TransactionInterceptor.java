package io.graphoenix.core.transaction;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
@Priority(0)
@Interceptor
public class TransactionInterceptor {

    private final TransactionInterceptorProcessor transactionInterceptorProcessor;

    @Inject
    public TransactionInterceptor(TransactionInterceptorProcessor transactionInterceptorProcessor) {
        this.transactionInterceptorProcessor = transactionInterceptorProcessor;
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocationContext) {
        return transactionInterceptorProcessor.aroundInvoke(invocationContext);
    }
}
