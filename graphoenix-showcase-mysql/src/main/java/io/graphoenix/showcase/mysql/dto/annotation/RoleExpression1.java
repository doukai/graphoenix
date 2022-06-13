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
public @interface RoleExpression1 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression name() default @StringExpression;

  RoleTypeExpression type() default @RoleTypeExpression;

  boolean isDeprecated() default false;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  TimestampExpression createTime() default @TimestampExpression;

  StringExpression updateUserId() default @StringExpression;

  TimestampExpression updateTime() default @TimestampExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  String[] groupBy() default {};

  RoleOrderBy1 orderBy() default @RoleOrderBy1;

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

  UserExpression2 users() default @UserExpression2;

  RoleExpression2[] exs() default {};
}
