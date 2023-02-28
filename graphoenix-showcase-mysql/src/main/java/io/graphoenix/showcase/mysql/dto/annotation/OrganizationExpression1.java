package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationExpression1 {
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

  IntExpression orgLevel3() default @IntExpression;

  BooleanExpression roleDisable() default @BooleanExpression;

  String[] groupBy() default {};

  OrganizationOrderBy1 orderBy() default @OrganizationOrderBy1;

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

  OrganizationExpression2 above() default @OrganizationExpression2;

  UserExpression2 users() default @UserExpression2;

  UserExpression2 rpcUsers() default @UserExpression2;

  StringExpression2 name() default @StringExpression2;

  StringExpression2 realmId() default @StringExpression2;

  StringExpression2 createUserId() default @StringExpression2;

  StringExpression2 updateUserId() default @StringExpression2;

  StringExpression2 createGroupId() default @StringExpression2;

  StringExpression2 __typename() default @StringExpression2;

  StringExpression2 nameMax() default @StringExpression2;

  StringExpression2 nameMin() default @StringExpression2;

  UserExpression2 userByOrg() default @UserExpression2;

  OrganizationExpression2 parent() default @OrganizationExpression2;

  ContainerTypeExpression2 containerType() default @ContainerTypeExpression2;

  OrganizationExpression2[] exs() default {};
}
