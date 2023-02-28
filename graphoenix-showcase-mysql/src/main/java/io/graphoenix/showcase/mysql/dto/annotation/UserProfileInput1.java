package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserProfileInput1 {
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

  StringInput2 userId() default "";

  StringInput2 email() default "";

  StringInput2 address() default "";

  StringInput2 qq() default "";

  UserInput2 user() default @UserInput2;

  UserInput2 rpcUser() default @UserInput2;

  StringInput2 realmId() default "";

  StringInput2 createUserId() default "";

  StringInput2 updateUserId() default "";

  StringInput2 createGroupId() default "";

  StringInput2 __typename() default "";

  StringInput2 userIdMax() default "";

  StringInput2 userIdMin() default "";

  StringInput2 emailMax() default "";

  StringInput2 emailMin() default "";

  StringInput2 addressMax() default "";

  StringInput2 addressMin() default "";

  StringInput2 qqMax() default "";

  StringInput2 qqMin() default "";

  UserProfileInput2[] list() default {};

  String $list() default "";
}
