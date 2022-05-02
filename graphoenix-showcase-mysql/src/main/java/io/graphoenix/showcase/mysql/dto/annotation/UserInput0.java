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
public @interface UserInput0 {
  String id() default "";

  String login() default "";

  String password() default "";

  String name() default "";

  int age() default 0;

  boolean disable() default false;

  Sex sex() default Sex.MAN;

  int organizationId() default 0;

  String[] phones() default {};

  int[] test1() default {};

  boolean[] test2() default {};

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  String userDetail2() default "";

  String $id() default "";

  String $login() default "";

  String $password() default "";

  String $name() default "";

  String $age() default "";

  String $disable() default "";

  String $sex() default "";

  String $organizationId() default "";

  String $organization() default "";

  String $roles() default "";

  String $phones() default "";

  String $test1() default "";

  String $test2() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $rolesAggregate() default "";

  String $rolesConnection() default "";

  String $userDetail2() default "";

  OrganizationInput1 organization() default @OrganizationInput1;

  RoleInput1[] roles() default {};

  RoleInput1 rolesAggregate() default @RoleInput1;

  RoleConnectionInput1 rolesConnection() default @RoleConnectionInput1;
}
