package io.graphoenix.showcase.mysql.inject;

import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

public class Class5 extends AClass2 implements IClass2 {

    private Mono<Class6> class6;
    private Mono<Class1> class1;

    @Inject
    public Class5(Mono<Class6> class6, Mono<Class1> class1) {
        this.class6 = class6;
        this.class1 = class1;
    }
}
