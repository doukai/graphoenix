package io.graphoenix.showcase.mysql.inject;

import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

public class Class5 extends AClass2 implements IClass2 {

    private PublisherBuilder<Class6> class6;
    private PublisherBuilder<Class1> class1;

    @Inject
    public Class5(PublisherBuilder<Class6> class6, PublisherBuilder<Class1> class1) {
        this.class6 = class6;
        this.class1 = class1;
    }
}
