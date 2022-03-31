package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __DirectiveInput1 {
  String name() default "";

  int schemaId() default 0;

  String description() default "";

  __DirectiveLocation[] locations() default {};

  boolean onOperation() default false;

  boolean onFragment() default false;

  boolean onField() default false;

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  String $name() default "";

  String $schemaId() default "";

  String $description() default "";

  String $locations() default "";

  String $args() default "";

  String $onOperation() default "";

  String $onFragment() default "";

  String $onField() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $argsAggregate() default "";

  String $argsConnection() default "";

  __InputValueInput2[] args() default {};

  __InputValueInput2 argsAggregate() default @__InputValueInput2;

  __InputValueConnectionInput2 argsConnection() default @__InputValueConnectionInput2;
}
