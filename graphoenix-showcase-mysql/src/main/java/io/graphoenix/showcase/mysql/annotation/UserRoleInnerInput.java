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
public @interface UserRoleInnerInput {
  int roleId() default 0;

  int id() default 0;

  int userId() default 0;

  String $roleId() default "";

  String $id() default "";

  String $userId() default "";
}
