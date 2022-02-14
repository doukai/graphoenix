package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Name("UserInput")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface UserInput0 {
  boolean isDeprecated() default false;

  Sex sex() default Sex.MAN;

  String[] phones() default {};

  String login() default "";

  String userDetail2() default "";

  int version() default 0;

  int organizationId() default 0;

  String password() default "";

  boolean disable() default false;

  String name() default "";

  String id() default "";

  int age() default 0;

  String $isDeprecated() default "";

  String $sex() default "";

  String $roles() default "";

  String $phones() default "";

  String $login() default "";

  String $userDetail2() default "";

  String $version() default "";

  String $organizationId() default "";

  String $password() default "";

  String $disable() default "";

  String $organization() default "";

  String $name() default "";

  String $id() default "";

  String $age() default "";

  RoleInput1[] roles() default {};

  OrganizationInput1 organization() default @OrganizationInput1;
}
