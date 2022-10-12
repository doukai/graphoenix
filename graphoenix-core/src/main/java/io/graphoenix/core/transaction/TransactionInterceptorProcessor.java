package io.graphoenix.core.transaction;

import jakarta.interceptor.InvocationContext;

public interface TransactionInterceptorProcessor {
    Object aroundInvoke(InvocationContext invocationContext);
}
