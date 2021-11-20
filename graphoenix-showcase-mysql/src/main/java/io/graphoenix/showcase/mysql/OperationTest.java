package io.graphoenix.showcase.mysql;

import io.graphoenix.spi.annotation.Mutation;
import io.graphoenix.spi.annotation.Operation;
import io.graphoenix.spi.annotation.Query;
import io.graphoenix.spi.annotation.Subscription;

@Operation
public interface OperationTest {

    @Query
    String query();

    @Mutation
    String mutation();

    @Subscription
    String subscription();
}
