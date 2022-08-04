package io.graphoenix.showcase.mysql.client;

import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.showcase.mysql.grpc.QueryIntroTypeRequest;
import io.graphoenix.showcase.mysql.grpc.ReactorQueryTypeServiceGrpc;
import io.graphoenix.showcase.mysql.grpc.StringExpression;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GraphQLClient {

    public static void main(String[] args) {

        ManagedChannel localhost = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext()
                .build();

        ReactorQueryTypeServiceGrpc.ReactorQueryTypeServiceStub reactorQueryTypeServiceStub = ReactorQueryTypeServiceGrpc.newReactorStub(localhost);

        reactorQueryTypeServiceStub.introType(QueryIntroTypeRequest.newBuilder().setName(StringExpression.newBuilder().setVal("User").build()).build())
                .doOnSuccess(r -> System.out.println(r.getIntroType().getName()))
                .block();

//        BatchLoader<Long, User> userBatchLoader = (BatchLoader<Long, User>) userIds -> CompletableFuture.supplyAsync(() -> {
//            return userManager.loadUsersById(userIds);
//        });
//
//        DataLoader<Long, User> userLoader = DataLoaderFactory.newDataLoader(userBatchLoader);


    }
}
