package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserProfileExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression userId() default @StringExpression;

  StringExpression email() default @StringExpression;

  StringExpression address() default @StringExpression;

  StringExpression qq() default @StringExpression;

  BooleanExpression isDeprecated() default @BooleanExpression;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  String[] groupBy() default {};

  UserProfileOrderBy0 orderBy() default @UserProfileOrderBy0;

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

  StringExpression1 userId() default @StringExpression1;

  StringExpression1 email() default @StringExpression1;

  StringExpression1 address() default @StringExpression1;

  StringExpression1 qq() default @StringExpression1;

  UserExpression1 user() default @UserExpression1;

  UserExpression1 rpcUser() default @UserExpression1;

  StringExpression1 realmId() default @StringExpression1;

  StringExpression1 createUserId() default @StringExpression1;

  StringExpression1 updateUserId() default @StringExpression1;

  StringExpression1 createGroupId() default @StringExpression1;

  StringExpression1 __typename() default @StringExpression1;

  StringExpression1 userIdMax() default @StringExpression1;

  StringExpression1 userIdMin() default @StringExpression1;

  StringExpression1 emailMax() default @StringExpression1;

  StringExpression1 emailMin() default @StringExpression1;

  StringExpression1 addressMax() default @StringExpression1;

  StringExpression1 addressMin() default @StringExpression1;

  StringExpression1 qqMax() default @StringExpression1;

  StringExpression1 qqMin() default @StringExpression1;

  UserProfileExpression1[] exs() default {};
}
