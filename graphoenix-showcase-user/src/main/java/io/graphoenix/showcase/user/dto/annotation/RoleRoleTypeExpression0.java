package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.core.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleRoleTypeExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  IntExpression roleId() default @IntExpression;

  RoleTypeExpression type() default @RoleTypeExpression;

  BooleanExpression isDeprecated() default @BooleanExpression;

  IntExpression version() default @IntExpression;

  IntExpression realmId() default @IntExpression;

  StringExpression createUserId() default @StringExpression;

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  StringExpression metaInfo() default @StringExpression;

  String[] groupBy() default {};

  RoleRoleTypeOrderBy0 orderBy() default @RoleRoleTypeOrderBy0;

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
}
