package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.RoleType;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface RoleInput1 {
  String id() default "";

  String name() default "";

  RoleType[] type() default {};

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  String $id() default "";

  String $name() default "";

  String $type() default "";

  String $users() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $usersAggregate() default "";

  String $usersConnection() default "";

  UserInput2[] users() default {};

  UserInput2 usersAggregate() default @UserInput2;

  UserConnectionInput2 usersConnection() default @UserConnectionInput2;
}
