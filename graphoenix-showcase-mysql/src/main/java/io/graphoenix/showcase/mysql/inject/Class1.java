package io.graphoenix.showcase.mysql.inject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Class1 {

    private Class2 class2;
    private Class3 class3;

    @Inject
    public Class1(Class2 class2, Class3 class3) {
        this.class2 = class2;
        this.class3 = class3;
    }
}
