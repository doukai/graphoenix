package io.graphoenix.showcase.mysql.client;

import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GraphQLClient {

    private static final List<User> userList = new ArrayList<>();

    static class User {
        Long id;
        String name;
        Integer age;
        Long bossId;

        public User(Long id, String name, Integer age, Long bossId) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.bossId = bossId;
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        userList.add(new User(1L, "aa", 18, 3L));
        userList.add(new User(2L, "bb", 19, 4L));
        userList.add(new User(3L, "cc", 20, 5L));
        userList.add(new User(4L, "dd", 31, 5L));
        userList.add(new User(5L, "ee", 3, 2L));

//        ManagedChannel localhost = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext()
//                .build();
//
//        ReactorQueryTypeServiceGrpc.ReactorQueryTypeServiceStub reactorQueryTypeServiceStub = ReactorQueryTypeServiceGrpc.newReactorStub(localhost);
//
//        reactorQueryTypeServiceStub.introType(QueryIntroTypeRequest.newBuilder().setName(StringExpression.newBuilder().setVal("User").build()).build())
//                .doOnSuccess(r -> System.out.println(r.getIntroType().getName()))
//                .block();

        BatchLoader<Long, User> userBatchLoader = userIds -> CompletableFuture.supplyAsync(() -> userList.stream().filter(user -> userIds.contains(user.id)).collect(Collectors.toList()));

        DataLoader<Long, User> userLoader = DataLoaderFactory.newDataLoader(userBatchLoader);
        userLoader.load(1L)
                .thenAccept(user -> {
                    System.out.println("user = " + user);
                    userLoader.load(user.bossId)
                            .thenAccept(invitedBy -> {
                                userLoader.load(invitedBy.bossId)
                                        .thenAccept(invitedBy2 -> {
                                            System.out.println("bossId = " + invitedBy2.bossId);
                                        });
                            });
                });

        userLoader.load(2L)
                .thenAccept(user -> {
                    System.out.println("user = " + user);
                    userLoader.load(user.bossId)
                            .thenAccept(invitedBy -> {
                                System.out.println("bossId = " + invitedBy);
                            });
                });

        userLoader.load(5L)
                .thenAccept(user -> {
                    System.out.println("user = " + user);
                    userLoader.load(user.bossId)
                            .thenAccept(invitedBy -> {
                                System.out.println("bossId = " + invitedBy);
                            });
                });

        userLoader.dispatch();

        Mono.just("aaa").toFuture();
    }
}
