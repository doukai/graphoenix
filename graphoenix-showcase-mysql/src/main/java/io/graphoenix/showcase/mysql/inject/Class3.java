package io.graphoenix.showcase.mysql.inject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import reactor.core.publisher.Mono;

@ApplicationScoped
public class Class3 {

    @Inject
    public Provider<Mono<Class1>> class1;

    private Provider<Class2> class2;

    @Inject
    public Class3(Provider<Class2> class2) {
        this.class2 = class2;
    }

    @Produces
    @RequestScoped
    @Named("no")
    public Mono<Class6> class6() {
        return Mono.just(new Class6());
    }

    @Produces
    @Dependent
    public Class5 class5() {
        return new Class5(this::class6, class1);
    }
}
