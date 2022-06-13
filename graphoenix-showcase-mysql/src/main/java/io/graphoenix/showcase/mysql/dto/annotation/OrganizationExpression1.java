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
public @interface OrganizationExpression1 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  IntExpression aboveId() default @IntExpression;

  StringExpression name() default @StringExpression;

  boolean isDeprecated() default false;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  TimestampExpression createTime() default @TimestampExpression;

  StringExpression updateUserId() default @StringExpression;

  TimestampExpression updateTime() default @TimestampExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  IntExpression orgLevel2() default @IntExpression;

  IntExpression orgLevel3() default @IntExpression;

  boolean roleDisable() default false;

  String[] groupBy() default {};

  OrganizationOrderBy1 orderBy() default @OrganizationOrderBy1;

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

  OrganizationExpression2 above() default @OrganizationExpression2;

  UserExpression2 users() default @UserExpression2;

  UserExpression2 userByOrg() default @UserExpression2;

  OrganizationExpression2 parent() default @OrganizationExpression2;

  OrganizationExpression2[] exs() default {};
}
