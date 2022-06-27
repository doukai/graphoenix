package io.graphoenix.showcase.mysql.inject;

import io.graphoenix.showcase.mysql.annotation.Test;
import io.graphoenix.showcase.mysql.annotation.Test2;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@RequestScoped
@Named("yes")
public class Class1 extends AClass1 implements IClass1 {

    private Class2 class2;
    private Class3 class3;

    @Inject
    @Test
    @Test2
    public Class1(Class2 class2, Class3 class3) {
        this.class2 = class2;
        this.class3 = class3;
    }

    @Test
    @Test2
    public String test(String a, int b) throws Exception {
        return "hello";
    }
}
