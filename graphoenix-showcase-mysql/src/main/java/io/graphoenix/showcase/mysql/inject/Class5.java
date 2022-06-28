package io.graphoenix.showcase.mysql.inject;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import reactor.core.publisher.Mono;

public class Class5 extends AClass2 implements IClass2 {

    private Provider<Mono<Class6>> class6;
    private Provider<Mono<Class1>> class1;

    @Inject
    public Class5(Provider<Mono<Class6>> class6, Provider<Mono<Class1>> class1) {
        this.class6 = class6;
        this.class1 = class1;
    }
}
