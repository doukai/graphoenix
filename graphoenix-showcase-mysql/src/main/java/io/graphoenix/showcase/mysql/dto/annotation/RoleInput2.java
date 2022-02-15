package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface RoleInput2 {
  boolean isDeprecated() default false;

  String name() default "";

  String id() default "";

  int version() default 0;

  String $isDeprecated() default "";

  String $name() default "";

  String $id() default "";

  String $version() default "";

  String $users() default "";
}
