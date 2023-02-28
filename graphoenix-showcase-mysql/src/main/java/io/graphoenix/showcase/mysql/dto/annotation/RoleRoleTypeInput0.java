package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.RoleType;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleRoleTypeInput0 {
  String id() default "";

  int roleId() default 0;

  RoleType type() default RoleType.ADMIN;

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  String $id() default "";

  String $roleId() default "";

  String $type() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  RoleRoleTypeInput1[] list() default {};

  String $list() default "";
}
