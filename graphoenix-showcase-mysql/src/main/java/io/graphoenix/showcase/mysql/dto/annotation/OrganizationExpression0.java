package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  IntExpression aboveId() default @IntExpression;

  StringExpression name() default @StringExpression;

  BooleanExpression isDeprecated() default @BooleanExpression;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  IntExpression orgLevel2() default @IntExpression;

  IntExpression orgLevel3() default @IntExpression;

  BooleanExpression roleDisable() default @BooleanExpression;

  String[] groupBy() default {};

  OrganizationOrderBy0 orderBy() default @OrganizationOrderBy0;

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

  OrganizationExpression1 above() default @OrganizationExpression1;

  UserExpression1 users() default @UserExpression1;

  UserExpression1 userByOrg() default @UserExpression1;

  OrganizationExpression1 parent() default @OrganizationExpression1;

  OrganizationExpression1[] exs() default {};
}
