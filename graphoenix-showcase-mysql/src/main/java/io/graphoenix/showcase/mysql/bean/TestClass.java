package io.graphoenix.showcase.mysql.bean;

import javax.validation.constraints.NotNull;

public class TestClass {

    @NotNull(message = "test")
    public String getName(int id) {
        return "1";
    }
}
