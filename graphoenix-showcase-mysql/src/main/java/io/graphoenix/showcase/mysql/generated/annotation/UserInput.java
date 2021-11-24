package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserInput {
  int organizationId() default 0;

  String password() default "";

  boolean isDeprecated() default false;

  boolean disable() default false;

  Sex sex() default Sex.MAN;

  String name() default "";

  int id() default 0;

  String login() default "";

  int version() default 0;

  int age() default 0;

  String $organizationId() default "";

  String $password() default "";

  String $isDeprecated() default "";

  String $disable() default "";

  String $sex() default "";

  String $name() default "";

  String $id() default "";

  String $login() default "";

  String $version() default "";

  String $age() default "";
}
