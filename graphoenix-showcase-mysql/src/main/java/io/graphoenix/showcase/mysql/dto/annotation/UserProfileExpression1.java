package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserProfileExpression1 {
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

  UserProfileOrderBy1 orderBy() default @UserProfileOrderBy1;

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

  StringExpression2 userId() default @StringExpression2;

  StringExpression2 email() default @StringExpression2;

  StringExpression2 address() default @StringExpression2;

  StringExpression2 qq() default @StringExpression2;

  UserExpression2 user() default @UserExpression2;

  UserExpression2 rpcUser() default @UserExpression2;

  StringExpression2 realmId() default @StringExpression2;

  StringExpression2 createUserId() default @StringExpression2;

  StringExpression2 updateUserId() default @StringExpression2;

  StringExpression2 createGroupId() default @StringExpression2;

  StringExpression2 __typename() default @StringExpression2;

  StringExpression2 userIdMax() default @StringExpression2;

  StringExpression2 userIdMin() default @StringExpression2;

  StringExpression2 emailMax() default @StringExpression2;

  StringExpression2 emailMin() default @StringExpression2;

  StringExpression2 addressMax() default @StringExpression2;

  StringExpression2 addressMin() default @StringExpression2;

  StringExpression2 qqMax() default @StringExpression2;

  StringExpression2 qqMin() default @StringExpression2;

  UserProfileExpression2[] exs() default {};
}
