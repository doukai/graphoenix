package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __TypeInput0 {
  boolean isDeprecated() default false;

  __TypeKind kind() default __TypeKind.SCALAR;

  String __typename() default "";

  String description() default "";

  int version() default 0;

  int schemaId() default 0;

  String name() default "";

  String ofTypeName() default "";

  String $interfaces() default "";

  String $isDeprecated() default "";

  String $possibleTypes() default "";

  String $kind() default "";

  String $__typename() default "";

  String $description() default "";

  String $version() default "";

  String $inputFields() default "";

  String $schemaId() default "";

  String $name() default "";

  String $fields() default "";

  String $ofType() default "";

  String $enumValues() default "";

  String $ofTypeName() default "";

  __TypeInput1[] interfaces() default {};

  __TypeInput1[] possibleTypes() default {};

  __InputValueInput1[] inputFields() default {};

  __FieldInput1[] fields() default {};

  __TypeInput1 ofType() default @__TypeInput1;

  __EnumValueInput1[] enumValues() default {};
}
