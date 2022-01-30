package io.graphoenix.showcase.mysql.module;

import javax.validation.constraints.NotNull;

public class TestClass {

    @NotNull(message = "test")
    public String getName(int id) {
        return "1";
    }
}
