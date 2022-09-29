package io.graphoenix.core.transaction;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
@Priority(0)
@Interceptor
public class TransactionInterceptor {

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocationContext) throws NoSuchMethodException {
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
