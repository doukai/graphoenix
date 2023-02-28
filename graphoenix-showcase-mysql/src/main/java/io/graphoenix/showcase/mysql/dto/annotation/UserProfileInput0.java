package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserProfileInput0 {
  String id() default "";

  String userId() default "";

  String email() default "";

  String address() default "";

  String qq() default "";

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

  String $userId() default "";

  String $email() default "";

  String $address() default "";

  String $qq() default "";

  String $user() default "";

  String $rpcUser() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  StringInput1 userId() default "";

  StringInput1 email() default "";

  StringInput1 address() default "";

  StringInput1 qq() default "";

  UserInput1 user() default @UserInput1;

  UserInput1 rpcUser() default @UserInput1;

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  StringInput1 userIdMax() default "";

  StringInput1 userIdMin() default "";

  StringInput1 emailMax() default "";

  StringInput1 emailMin() default "";

  StringInput1 addressMax() default "";

  StringInput1 addressMin() default "";

  StringInput1 qqMax() default "";

  StringInput1 qqMin() default "";

  UserProfileInput1[] list() default {};

  String $list() default "";
}
