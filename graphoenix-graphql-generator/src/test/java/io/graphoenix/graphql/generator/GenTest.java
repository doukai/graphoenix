package io.graphoenix.graphql.generator;

import io.graphoenix.graphql.generator.document.*;
import io.graphoenix.graphql.generator.operation.Operation;
import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.Arrays;

public class GenTest {

    @Test
    void name() {

        STGroup test = new STGroupFile("stg/document/ObjectType.stg");
        ST st = test.getInstanceOf("objectTypeDefinition");
        ObjectType objectType = new ObjectType();
        objectType.setName("User");
        Field field = new Field();
        field.setName("name");
        field.setTypeName("String");

        InputValue inputValue3 = new InputValue();
        inputValue3.setName("arg1");
        inputValue3.setDefaultValue("d1");
        inputValue3.setTypeName("String");
        inputValue3.setDescription("dddddd1");

        InputValue inputValue4 = new InputValue();
        inputValue4.setName("arg2");
        inputValue4.setDefaultValue("d2");
        inputValue4.setTypeName("Int");
        inputValue4.setDescription("eeeee3");
        field.setArguments(Arrays.asList(inputValue3, inputValue4));


        Field field2 = new Field();
        field2.setName("age");
        field2.setTypeName("Int");
        objectType.setFields(Arrays.asList(field, field2));
        objectType.setInterfaces(Arrays.asList("BaseEntity", "People"));

        Directive directive = new Directive();
        directive.setName("directive1");

        InputValue inputValue = new InputValue();
        inputValue.setName("arg1");
        inputValue.setDefaultValue("d1");
        inputValue.setTypeName("String");
        inputValue.setDescription("dddddd1");

        InputValue inputValue2 = new InputValue();
        inputValue2.setName("arg2");
        inputValue2.setDefaultValue("d2");
        inputValue2.setTypeName("Int");
        inputValue2.setDescription("eeeee3");
        directive.setArguments(Arrays.asList(inputValue, inputValue2));

        Directive directive2 = new Directive();
        directive2.setName("directive2");

//        objectType.setDescription("user entity");
        st.add("objectType", objectType);
        System.out.println(st.render());
    }

    @Test
    void test2() {

        STGroup test = new STGroupFile("stg/document/EnumType.stg");
        ST st = test.getInstanceOf("enumTypeDefinition");
        EnumType enumType = new EnumType();
        enumType.setName("Sex");
        EnumValue enumValue = new EnumValue();
        enumValue.setName("man");
        enumValue.setDescription("eeeeeeew");

        Directive directive = new Directive();
        directive.setName("directive1");

        InputValue inputValue = new InputValue();
        inputValue.setName("arg1");
        inputValue.setDefaultValue("d1");
        inputValue.setTypeName("String");
        inputValue.setDescription("dddddd1");

        InputValue inputValue2 = new InputValue();
        inputValue2.setName("arg2");
        inputValue2.setDefaultValue("d2");
        inputValue2.setTypeName("Int");
        inputValue2.setDescription("eeeee3");
        directive.setArguments(Arrays.asList(inputValue, inputValue2));


        EnumValue enumValue2 = new EnumValue();
        enumValue2.setName("woman");
        enumValue2.setDescription("asdfsadf");

        enumType.setEnumValues(Arrays.asList(enumValue, enumValue2));
        enumType.setDescription("ccccccccccccccc");
        st.add("enumType", enumType);
        System.out.println(st.render());
    }

    @Test
    void test3() {

        STGroup test = new STGroupFile("stg/document/Directive.stg");
        ST st = test.getInstanceOf("directiveDefinition");

        Directive directiveDefinition = new Directive();
        directiveDefinition.setName("test");


        InputValue inputValue = new InputValue();
        inputValue.setName("arg1");
        inputValue.setDefaultValue("d1");
        inputValue.setTypeName("String");
        inputValue.setDescription("dddddd1");

        InputValue inputValue2 = new InputValue();
        inputValue2.setName("arg2");
        inputValue2.setDefaultValue("d2");
        inputValue2.setTypeName("Int");
        inputValue2.setDescription("eeeee3");

        directiveDefinition.setArguments(Arrays.asList(inputValue, inputValue2));
        directiveDefinition.setDirectiveLocations(Arrays.asList("AAAA", "BBB"));


        st.add("directive", directiveDefinition);
        System.out.println(st.render());
    }

    @Test
    void test4() {
        Operation operation = new Operation()
                .setOperationType("mutation")
                .setName("updateTest")
                .setDirectives(Arrays.asList("@skip", "@skip2"))
                .setVariableDefinitions(Arrays.asList(new InputValue().setName("arg1").setTypeName("Type1"), new InputValue().setName("arg2").setTypeName("Type2")));

        System.out.println(operation);
    }
}
