package io.graphoenix.showcase.mysql.inject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public class Class5 {

    private Class6 class6;

    @Inject
    public Class5(Class6 class6) {
        this.class6 = class6;
    }
}
