package io.graphoenix.showcase.mysql.bean;

import io.graphoenix.showcase.mysql.annotation.Aspect;

public class TestClass {

    @Aspect(value = "test")
    public String getName(int id) {
        return "1";
    }
}
