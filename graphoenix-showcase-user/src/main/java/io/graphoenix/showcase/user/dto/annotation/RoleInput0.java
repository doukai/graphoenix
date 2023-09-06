package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.showcase.user.dto.enumType.RoleType;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleInput0 {
  String id() default "";

  String name() default "";

  RoleType[] type() default {};

  boolean isDeprecated() default false;

  int version() default 0;

  int realmId() default 0;

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  String metaInfo() default "";

  String $id() default "";

  String $name() default "";

  String $type() default "";

  String $users() default "";

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

  String $roleRoleType() default "";

  String $roleRoleTypeAggregate() default "";

  String $roleRoleTypeConnection() default "";

  String $userRole() default "";

  String $userRoleAggregate() default "";

  String $userRoleConnection() default "";

  String $metaInfo() default "";
}
