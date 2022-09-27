package io.graphoenix.r2dbc.connector.transaction;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.util.Map;

@TransactionScoped
public class R2dbcTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry {

    private int status;

    private Map<Object, Object> resourceMap;

    @Override
    public Object getTransactionKey() {
        return null;
    }

    @Override
    public void putResource(Object key, Object value) {
        this.resourceMap.put(key, value);
    }

    @Override
    public Object getResource(Object key) {
        return this.resourceMap.get(key);
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {

    }

    @Override
    public int getTransactionStatus() {
        return this.status;
    }

    @Override
    public void setRollbackOnly() {

    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }
}
