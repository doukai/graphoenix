package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface OrganizationInput {
  OrganizationInnerInput parent() default @OrganizationInnerInput;

  int aboveId() default 0;

  boolean isDeprecated() default false;

  UserInnerInput[] userByOrg() default {};

  OrganizationInnerInput above() default @OrganizationInnerInput;

  String name() default "";

  int orgLevel2() default 0;

  boolean roleDisable() default false;

  int id() default 0;

  int version() default 0;

  UserInnerInput[] users() default {};

  String $parent() default "";

  String $aboveId() default "";

  String $isDeprecated() default "";

  String $userByOrg() default "";

  String $above() default "";

  String $name() default "";

  String $orgLevel2() default "";

  String $roleDisable() default "";

  String $id() default "";

  String $version() default "";

  String $users() default "";
}
