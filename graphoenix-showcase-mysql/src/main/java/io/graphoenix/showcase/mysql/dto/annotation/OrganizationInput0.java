package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationInput0 {
  String id() default "";

  int aboveId() default 0;

  String name() default "";

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  int[] orgLevel3() default {};

  boolean[] roleDisable() default {};

  String $id() default "";

  String $aboveId() default "";

  String $above() default "";

  String $users() default "";

  String $rpcUsers() default "";

  String $name() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $usersAggregate() default "";

  String $usersConnection() default "";

  String $orgLevel3() default "";

  String $roleDisable() default "";

  String $userByOrg() default "";

  String $parent() default "";

  OrganizationInput1 above() default @OrganizationInput1;

  UserInput1[] users() default {};

  UserInput1[] rpcUsers() default {};

  UserInput1[] userByOrg() default {};

  OrganizationInput1 parent() default @OrganizationInput1;

  OrganizationInput1[] list() default {};

  String $list() default "";
}
