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
public @interface OrganizationInput1 {
  boolean isDeprecated() default false;

  String __typename() default "";

  boolean roleDisable() default false;

  int version() default 0;

  int aboveId() default 0;

  String name() default "";

  int orgLevel2() default 0;

  int[] orgLevel3() default {};

  String id() default "";

  String $parent() default "";

  String $isDeprecated() default "";

  String $userByOrg() default "";

  String $__typename() default "";

  String $roleDisable() default "";

  String $version() default "";

  String $users() default "";

  String $aboveId() default "";

  String $above() default "";

  String $name() default "";

  String $orgLevel2() default "";

  String $orgLevel3() default "";

  String $id() default "";

  OrganizationInput2 parent() default @OrganizationInput2;

  UserInput2[] userByOrg() default {};

  UserInput2[] users() default {};

  OrganizationInput2 above() default @OrganizationInput2;
}
