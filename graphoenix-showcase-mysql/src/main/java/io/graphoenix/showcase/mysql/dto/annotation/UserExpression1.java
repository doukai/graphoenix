package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.Arguments;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Arguments
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

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  StringExpression userDetail2() default @StringExpression;

  String[] groupBy() default {};

  UserOrderBy1 orderBy() default @UserOrderBy1;

  int first() default 0;

  String $first() default "";

  int last() default 0;

  String $last() default "";

  int offset() default 0;

  String $offset() default "";

  String after() default "";

  String before() default "";

  String $after() default "";

  String $before() default "";

  OrganizationExpression2 organization() default @OrganizationExpression2;

  RoleExpression2 roles() default @RoleExpression2;

  UserExpression2[] exs() default {};
}
