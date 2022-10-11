package io.graphoenix.showcase.test;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.List;

public class MonoTest {

    List<String> stringList = new ArrayList<>();

    Mono<List<String>> mono = Mono.just(stringList);

    @Test
    void test1() {
//        String key = "key";
//        String key2 = "key2";
//        Map<String, Object> p = new HashMap<>();
//        String block = Mono.from(Mono.deferContextual(ctx ->
//                        Mono.just("$" + ctx.get(key))))
//                .contextWrite(Context.of(key, "myValue"))
//                .block();
//
//        System.out.println(block);
//        GraphQLConfig object = new GraphQLConfig();
//        Package objPackage = MonoTest.class.getPackage();
//        //examine the package object
//        String name = objPackage.getSpecificationTitle();
//        String version = objPackage.getSpecificationVersion();
//        //some jars may use 'Implementation Version' entries in the manifest instead
//        System.out.println("Package name: " + name);
//        System.out.println("Package version: " + version);

//        String[] a = {"a", "b", "c", "d"};
//        System.out.println(
//                Flux.fromArray(a)
//                        .map(item -> Flux.fromStream(Stream.of(a)).last().block())
////                .flatMap(stream -> Flux.fromIterable(stream))
//                        .last()
//                        .block()
//        );

//        getString("AAAA");
//        getString("BBBB");
//        mono.block();
//
//        ArrayList<String> arrayList = new ArrayList<>();
//        arrayList.add("CCCC");
//        arrayList.add("DDDD");
//        getStringList(arrayList).subscribe(list -> list.forEach(System.out::println));

//        Mono.just("1").then(Mono.fromRunnable(() -> {
//            throw new RuntimeException("test");
//        }))
//                .then(Mono.just("2"))
//                .onErrorResume(throwable -> Mono.just("b").doOnSuccess(System.out::println).then(Mono.error(throwable)))
//                .doOnSuccess(System.out::println)
//                .block();

//        String key = "key";
//        String key2 = "key2";
//        String block = Mono.from(
//
//                        Mono.just("$")
//                                .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(s + ctx.get(key))
//                                ))
//                                .contextWrite(Context.of(key, "myValue4"))
//                                .doOnSuccess(System.out::println)
//
//                ).then(
//
//                        Mono.just("$")
//                                .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(s + ctx.get(key))
//                                ))
//                                .contextWrite(Context.of(key, "myValue5"))
//                                .doOnSuccess(System.out::println)
//
//                ).then(
//                        Mono.just("$")
//                                .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(s + ctx.get(key))
//                                ))
//                                .doOnSuccess(System.out::println)
//
//                )
//                .contextWrite(Context.of(key, "myValue3"))
//                .contextWrite(Context.of(key, "myValue2"))
//                .contextWrite(Context.of(key, "myValue"))
//                .block();

        String key = "key";
//        Mono<String> stringMono = Mono.from(Mono.deferContextual(ctx ->
//                Mono.just("$" + ctx.get(key))));
//
//        Mono.from(stringMono.doOnSuccess(System.out::println))
//                .contextWrite(Context.of(key, NanoIdUtils.randomNanoId())).block();
//        Mono.from(stringMono.doOnSuccess(System.out::println))
//                .contextWrite(Context.of(key, NanoIdUtils.randomNanoId())).block();

        Provider<Mono<String>> provider = this::test2;

        provider.get().doOnSuccess(System.out::println).then()
                .switchIfEmpty(
                        Mono.usingWhen(
                                        provider.get(),
                                        s -> Mono.fromRunnable(() -> System.out.println(s)),
                                        s -> Mono.fromRunnable(() -> System.out.println(s)),
                                        (connection, throwable) -> {
                                            Logger.error(throwable);
                                            return Mono.empty();
                                        },
                                        connection -> Mono.empty()
                                )
                                .then()
                                .contextWrite(Context.of(key, NanoIdUtils.randomNanoId()))
                )
                .contextWrite(Context.of(key, NanoIdUtils.randomNanoId()))
                .block();


    }

    Mono<String> test2() {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty("key")));
    }

    Mono<String> getString(String content) {

        stringList.add(content);
        int index = stringList.size() - 1;
        return mono.map(list -> list.get(index));
    }

    Mono<List<String>> getStringList(List<String> contents) {

        return Flux.fromIterable(contents).flatMap(this::getString).collectList();
    }

}
