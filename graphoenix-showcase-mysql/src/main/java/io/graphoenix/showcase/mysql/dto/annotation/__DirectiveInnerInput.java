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
public @interface __DirectiveInnerInput {
  boolean onFragment() default false;

  boolean isDeprecated() default false;

  int schemaId() default 0;

  String name() default "";

  String description() default "";

  __DirectiveLocation[] locations() default {};

  boolean onOperation() default false;

  int version() default 0;

  boolean onField() default false;

  String $onFragment() default "";

  String $isDeprecated() default "";

  String $schemaId() default "";

  String $name() default "";

  String $description() default "";

  String $locations() default "";

  String $onOperation() default "";

  String $version() default "";

  String $onField() default "";
}
