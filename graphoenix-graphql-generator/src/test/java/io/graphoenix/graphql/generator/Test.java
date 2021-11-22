package io.graphoenix.graphql.generator;

import io.graphoenix.graphql.generator.operation.ValueWithVariable;

import java.util.Arrays;

public class Test {

    @org.junit.jupiter.api.Test
    void test() {
        System.out.println(new ValueWithVariable("string1"));
        System.out.println(new ValueWithVariable(66));
        System.out.println(new ValueWithVariable(66.66));
        System.out.println(new ValueWithVariable(true));
        System.out.println(new ValueWithVariable(false));
        System.out.println(new ValueWithVariable(Sex.MAN));
        System.out.println(new ValueWithVariable(Sex.WOMAN));
        System.out.println(new ValueWithVariable(Arrays.asList(Sex.MAN, Sex.WOMAN)));
        System.out.println(new ValueWithVariable(Arrays.asList("string1", null)));
        System.out.println(new ValueWithVariable(new User("zhang", Sex.MAN, 18)));
    }
}
