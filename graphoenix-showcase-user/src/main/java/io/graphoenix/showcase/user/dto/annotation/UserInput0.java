package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.showcase.user.dto.enumType.Sex;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserInput0 {
  String id() default "";

  String login() default "";

  String password() default "";

  String name() default "";

  int age() default 0;

  boolean disabled() default false;

  Sex sex() default Sex.MAN;

  String[] mobileNumbers() default {};

  boolean isDeprecated() default false;

  int version() default 0;

  int realmId() default 0;

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  int organizationId() default 0;

  String $id() default "";

  String $login() default "";

  String $password() default "";

  String $name() default "";

  String $age() default "";

  String $disabled() default "";

  String $sex() default "";

  String $userProfile() default "";

  String $organization() default "";

  String $roles() default "";

  String $mobileNumbers() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $rolesAggregate() default "";

  String $rolesConnection() default "";

  String $organizationId() default "";

  String $userRole() default "";

  String $userRoleAggregate() default "";

  String $userRoleConnection() default "";

  String $userMobileNumbers() default "";

  String $userMobileNumbersAggregate() default "";

  String $userMobileNumbersConnection() default "";
}
