package io.graphoenix.showcase.test;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class MonoTest {

    @Test
    void test1() {
        String key = "key";
//        String key2 = "key2";
//        Map<String, Object> p = new HashMap<>();
        String block = Mono.from(Mono.deferContextual(ctx ->
                        Mono.just("$" + ctx.get(key))))
                .contextWrite(Context.of(key, "myValue"))
                .block();

        System.out.println(block);
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

    }
}
