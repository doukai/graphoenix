package io.graphoenix.showcase.mysql.inject;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class Class5 extends AClass2 implements IClass2 {

    private Instance<Class6> class6;
    private Instance<Class1> class1;

    @Inject
    public Class5(Instance<Class6> class6, Instance<Class1> class1) {
        this.class6 = class6;
        this.class1 = class1;
    }
}
