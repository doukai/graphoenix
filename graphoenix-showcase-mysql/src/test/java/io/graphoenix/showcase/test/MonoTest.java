package io.graphoenix.showcase.test;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class MonoTest {

    List<String> stringList = new ArrayList<>();

    Mono<List<String>> mono = Mono.just(stringList);

    @Test
    void test1() {
        String key = "key";
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

        getString("AAAA").subscribe(System.out::println);
        getString("BBBB").subscribe(System.out::println);
        mono.block();

    }

    Mono<String> getString(String content) {

        stringList.add(content);
        int index = stringList.size() - 1;
        return mono.map(list -> list.get(index));
    }
}
