package io.graphoenix.showcase.mysql.inject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class Class3 {

    @Inject
    private Provider<Class1> class1;

    private Provider<Class2> class2;

    @Inject
    public Class3(Provider<Class2> class2) {
        this.class2 = class2;
    }

    @Produces
    @ApplicationScoped
    public Class6 class6() {
        return new Class6();
    }

    @Produces
    @Dependent
    public Class5 class5() {
        return new Class5(class6());
    }
}
