package io.graphoenix.r2dbc.connector.transaction;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.TransactionSynchronizationRegistry;

@TransactionScoped
public class R2dbcTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry {

    @Override
    public Object getTransactionKey() {
        return null;
    }

    @Override
    public void putResource(Object key, Object value) {

    }

    @Override
    public Object getResource(Object key) {
        return null;
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {

    }

    @Override
    public int getTransactionStatus() {
        return 0;
    }

    @Override
    public void setRollbackOnly() {

    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }
}
