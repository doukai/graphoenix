package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.showcase.mysql.enumType.Sex;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface UserInput {
  int organizationId() default 0;

  String password() default "";

  boolean disable() default false;

  Sex sex() default Sex.MAN;

  OrganizationInnerInput organization() default @OrganizationInnerInput;

  RoleInnerInput[] roles() default {};

  String name() default "";

  String[] phones() default {};

  int id() default 0;

  String login() default "";

  int age() default 0;

  String $organizationId() default "";

  String $password() default "";

  String $disable() default "";

  String $sex() default "";

  String $organization() default "";

  String $roles() default "";

  String $name() default "";

  String $phones() default "";

  String $id() default "";

  String $login() default "";

  String $age() default "";
}
