package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Name("OrganizationInput")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface OrganizationInput1 {
  int aboveId() default 0;

  boolean isDeprecated() default false;

  String name() default "";

  int orgLevel2() default 0;

  int[] orgLevel3() default {};

  boolean roleDisable() default false;

  String id() default "";

  int version() default 0;

  String $parent() default "";

  String $aboveId() default "";

  String $isDeprecated() default "";

  String $userByOrg() default "";

  String $above() default "";

  String $name() default "";

  String $orgLevel2() default "";

  String $orgLevel3() default "";

  String $roleDisable() default "";

  String $id() default "";

  String $version() default "";

  String $users() default "";

  OrganizationInput2 parent() default @OrganizationInput2;

  UserInput2[] userByOrg() default {};

  OrganizationInput2 above() default @OrganizationInput2;

  UserInput2[] users() default {};
}
