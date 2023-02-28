package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationInput1 {
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

  String $containerType() default "";

  OrganizationInput2 above() default @OrganizationInput2;

  UserInput2[] users() default {};

  UserInput2[] rpcUsers() default {};

  StringInput2 name() default "";

  StringInput2 realmId() default "";

  StringInput2 createUserId() default "";

  StringInput2 updateUserId() default "";

  StringInput2 createGroupId() default "";

  StringInput2 __typename() default "";

  StringInput2 nameMax() default "";

  StringInput2 nameMin() default "";

  UserInput2[] userByOrg() default {};

  OrganizationInput2 parent() default @OrganizationInput2;

  ContainerTypeInput2 containerType() default @ContainerTypeInput2;

  OrganizationInput2[] list() default {};

  String $list() default "";
}
