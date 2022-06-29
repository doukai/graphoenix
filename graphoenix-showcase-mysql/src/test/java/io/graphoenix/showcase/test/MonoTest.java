package io.graphoenix.showcase.test;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MonoTest {

    @Test
    void test1() {
        String key = "key";
        String key2 = "key2";
        Map<String, Object> p = new HashMap<>();
        Mono.just("anything")
                .transform(s -> {
                    p.put(key2, "test");
                    return s;
                })
                .flatMap(s -> Mono.deferContextual(ctx ->
                        Mono.just(s + "$" + ctx.get(key2)))) // <1>
                .publishOn(Schedulers.newSingle("a")) // <3>
                .doOnNext(System.out::println)
                .map(a -> a.toUpperCase(Locale.ROOT))
                .doOnNext(System.out::println)
//                .transformDeferredContextual( // <2>
//                        (mono, context) -> mono.contextWrite(Context.of(key2, p.get(key2)))
//                )
//                .doFirst(() -> {
//                            p.put(key2, "test");
//                        }
//                )
                .transformDeferredContextual( // <2>
                        (mono, context) -> mono.contextWrite(Context.of(key2, p.get(key2)))
                )
                .contextWrite(Context.of(key, "myValue"))
                .block();

    }
}
