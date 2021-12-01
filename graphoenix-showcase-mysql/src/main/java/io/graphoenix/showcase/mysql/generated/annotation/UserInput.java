package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Sex;
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
  boolean isDeprecated() default false;

  Sex sex() default Sex.MAN;

  RoleInnerInput[] roles() default {};

  String[] phones() default {};

  String login() default "";

  int version() default 0;

  int organizationId() default 0;

  String password() default "";

  boolean disable() default false;

  OrganizationInnerInput organization() default @OrganizationInnerInput;

  String name() default "";

  int id() default 0;

  int age() default 0;

  String $isDeprecated() default "";

  String $sex() default "";

  String $roles() default "";

  String $phones() default "";

  String $login() default "";

  String $version() default "";

  String $organizationId() default "";

  String $password() default "";

  String $disable() default "";

  String $organization() default "";

  String $name() default "";

  String $id() default "";

  String $age() default "";
}
