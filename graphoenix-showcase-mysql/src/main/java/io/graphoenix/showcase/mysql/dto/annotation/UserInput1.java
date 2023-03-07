package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import null.dto.enumType.Sex;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserInput1 {
  String id() default "";

  String login() default "";

  String password() default "";

  String name() default "";

  int age() default 0;

  boolean disable() default false;

  Sex sex() default io.graphoenix.showcase.mysql.dto.enumType.Sex.MAN;

  int organizationId() default 0;

  String[] phones() default {};

  int[] test1() default {};

  boolean[] test2() default {};

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  String userDetail2() default "";

  String $id() default "";

  String $login() default "";

  String $password() default "";

  String $name() default "";

  String $age() default "";

  String $disable() default "";

  String $sex() default "";

  String $userProfile() default "";

  String $rpcUserProfile() default "";

  String $organizationId() default "";

  String $rpcOrganization() default "";

  String $organization() default "";

  String $roles() default "";

  String $phones() default "";

  String $test1() default "";

  String $test2() default "";

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

  String $userDetail2() default "";

  UserProfileInput2 userProfile() default @UserProfileInput2;

  UserProfileInput2 rpcUserProfile() default @UserProfileInput2;

  OrganizationInput2 rpcOrganization() default @OrganizationInput2;

  OrganizationInput2 organization() default @OrganizationInput2;

  RoleInput2[] roles() default {};

  UserInput2[] list() default {};

  String $list() default "";
}
