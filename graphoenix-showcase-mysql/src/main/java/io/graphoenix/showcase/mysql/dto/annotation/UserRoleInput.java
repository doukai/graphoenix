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
public @interface UserRoleInput {
  boolean isDeprecated() default false;

  int roleId() default 0;

  int id() default 0;

  int userId() default 0;

  int version() default 0;

  String $isDeprecated() default "";

  String $roleId() default "";

  String $id() default "";

  String $userId() default "";

  String $version() default "";
}
