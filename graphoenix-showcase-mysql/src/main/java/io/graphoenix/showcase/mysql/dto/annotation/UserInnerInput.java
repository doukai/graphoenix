package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface UserInnerInput {
  boolean isDeprecated() default false;

  Sex sex() default Sex.MAN;

  String[] phones() default {};

  String userDetail2() default "";

  String login() default "";

  int version() default 0;

  int organizationId() default 0;

  String password() default "";

  boolean disable() default false;

  String name() default "";

  String id() default "";

  int age() default 0;

  String $isDeprecated() default "";

  String $sex() default "";

  String $phones() default "";

  String $userDetail2() default "";

  String $login() default "";

  String $version() default "";

  String $organizationId() default "";

  String $password() default "";

  String $disable() default "";

  String $name() default "";

  String $id() default "";

  String $age() default "";
}
