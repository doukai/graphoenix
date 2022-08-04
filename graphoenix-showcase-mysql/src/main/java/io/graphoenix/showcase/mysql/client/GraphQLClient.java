package io.graphoenix.showcase.mysql.client;

import io.graphoenix.showcase.mysql.grpc.QueryIntroTypeRequest;
import io.graphoenix.showcase.mysql.grpc.ReactorQueryTypeServiceGrpc;
import io.graphoenix.showcase.mysql.grpc.StringExpression;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GraphQLClient {

    public static void main(String[] args) {

        ManagedChannel localhost = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext()
                .build();

        ReactorQueryTypeServiceGrpc.ReactorQueryTypeServiceStub reactorQueryTypeServiceStub = ReactorQueryTypeServiceGrpc.newReactorStub(localhost);

        reactorQueryTypeServiceStub.introType(QueryIntroTypeRequest.newBuilder().setName(StringExpression.newBuilder().setVal("User").build()).build())
                .doOnSuccess(r -> System.out.println(r.getIntroType().getName()))
                .block();

    }
}
