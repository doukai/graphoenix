package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserExpression1 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression login() default @StringExpression;

  StringExpression password() default @StringExpression;

  StringExpression name() default @StringExpression;

  IntExpression age() default @IntExpression;

  boolean disable() default false;

  SexExpression sex() default @SexExpression;

  IntExpression organizationId() default @IntExpression;

  StringExpression phones() default @StringExpression;

  IntExpression test1() default @IntExpression;

  boolean test2() default false;

  boolean isDeprecated() default false;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  TimestampExpression createTime() default @TimestampExpression;

  StringExpression updateUserId() default @StringExpression;

  TimestampExpression updateTime() default @TimestampExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  StringExpression userDetail2() default @StringExpression;

  String $id() default "";

  String $login() default "";

  String $password() default "";

  String $name() default "";

  String $age() default "";

  String $disable() default "";

  String $sex() default "";

  String $organizationId() default "";

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

  String $userDetail2() default "";

  String[] groupBy() default {};

  String $groupBy() default "";

  UserOrderBy1 orderBy() default @UserOrderBy1;

  String $orderBy() default "";

  int first() default 0;

  String $first() default "";

  int last() default 0;

  String $last() default "";

  int offset() default 0;

  String $offset() default "";

  int after() default 0;

  String $after() default "";

  int before() default 0;

  String $before() default "";

  OrganizationExpression2 organization() default @OrganizationExpression2;

  RoleExpression2 roles() default @RoleExpression2;
}
