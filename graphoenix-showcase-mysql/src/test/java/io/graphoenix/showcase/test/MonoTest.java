package io.graphoenix.showcase.test;

import io.graphoenix.core.config.GraphQLConfig;
import org.junit.jupiter.api.Test;

public class MonoTest {

    @Test
    void test1() {
//        String key = "key";
//        String key2 = "key2";
//        Map<String, Object> p = new HashMap<>();
//        Mono.just("anything")
//                .transform(s -> {
//                    p.put(key2, "test");
//                    return s;
//                })
//                .flatMap(s -> Mono.deferContextual(ctx ->
//                        Mono.just(s + "$" + ctx.get(key2)))) // <1>
//                .publishOn(Schedulers.newSingle("a")) // <3>
//                .doOnNext(System.out::println)
//                .map(a -> a.toUpperCase(Locale.ROOT))
//                .doOnNext(System.out::println)
////                .transformDeferredContextual( // <2>
////                        (mono, context) -> mono.contextWrite(Context.of(key2, p.get(key2)))
////                )
////                .doFirst(() -> {
////                            p.put(key2, "test");
////                        }
////                )
//                .transformDeferredContextual( // <2>
//                        (mono, context) -> mono.contextWrite(Context.of(key2, p.get(key2)))
//                )
//                .contextWrite(Context.of(key, "myValue"))
//                .block();
        GraphQLConfig object = new GraphQLConfig();
        Package objPackage = MonoTest.class.getPackage();
        //examine the package object
        String name = objPackage.getSpecificationTitle();
        String version = objPackage.getSpecificationVersion();
        //some jars may use 'Implementation Version' entries in the manifest instead
        System.out.println("Package name: " + name);
        System.out.println("Package version: " + version);
    }
}
