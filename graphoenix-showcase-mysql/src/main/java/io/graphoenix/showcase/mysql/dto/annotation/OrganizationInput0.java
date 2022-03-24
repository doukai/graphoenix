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
public @interface OrganizationInput0 {
  String id() default "";

  int aboveId() default 0;

  String name() default "";

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  int orgLevel2() default 0;

  int[] orgLevel3() default {};

  boolean roleDisable() default false;

  String $id() default "";

  String $aboveId() default "";

  String $above() default "";

  String $users() default "";

  String $name() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $orgLevel2() default "";

  String $orgLevel3() default "";

  String $roleDisable() default "";

  String $userByOrg() default "";

  String $parent() default "";

  OrganizationInput1 above() default @OrganizationInput1;

  UserInput1[] users() default {};

  UserInput1[] userByOrg() default {};

  OrganizationInput1 parent() default @OrganizationInput1;
}
