package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface OrganizationInnerInput {
  int aboveId() default 0;

  String name() default "";

  int id() default 0;

  String $aboveId() default "";

  String $name() default "";

  String $id() default "";
}
