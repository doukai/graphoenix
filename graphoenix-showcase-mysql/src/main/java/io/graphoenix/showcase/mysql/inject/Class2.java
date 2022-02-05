package io.graphoenix.showcase.mysql.inject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Class2 {

    private Class4 class4;

    @Inject
    public Class2(Class4 class4) {
        this.class4 = class4;
    }
}
