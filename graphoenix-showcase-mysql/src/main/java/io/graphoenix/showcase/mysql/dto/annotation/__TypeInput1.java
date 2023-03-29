package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeInput1 {
  String name() default "";

  int schemaId() default 0;

  __TypeKind kind() default __TypeKind.SCALAR;

  String description() default "";

  String ofTypeName() default "";

  String __typename() default "";

  String $name() default "";

  String $schemaId() default "";

  String $kind() default "";

  String $description() default "";

  String $fields() default "";

  String $interfaces() default "";

  String $possibleTypes() default "";

  String $enumValues() default "";

  String $inputFields() default "";

  String $ofTypeName() default "";

  String $ofType() default "";

  String $__typename() default "";

  String $fieldsAggregate() default "";

  String $fieldsConnection() default "";

  String $interfacesAggregate() default "";

  String $interfacesConnection() default "";

  String $possibleTypesAggregate() default "";

  String $possibleTypesConnection() default "";

  String $enumValuesAggregate() default "";

  String $enumValuesConnection() default "";

  String $inputFieldsAggregate() default "";

  String $inputFieldsConnection() default "";

  __FieldInput2[] fields() default {};

  __TypeInput2[] interfaces() default {};

  __TypeInput2[] possibleTypes() default {};

  __EnumValueInput2[] enumValues() default {};

  __InputValueInput2[] inputFields() default {};

  __TypeInput2 ofType() default @__TypeInput2;

  __TypeInput2[] list() default {};

  String $list() default "";
}
