package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypePossibleTypesExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression typeName() default @StringExpression;

  StringExpression possibleTypeName() default @StringExpression;

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

  __TypePossibleTypesOrderBy0 orderBy() default @__TypePossibleTypesOrderBy0;

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

  StringExpression1 typeName() default @StringExpression1;

  StringExpression1 possibleTypeName() default @StringExpression1;

  StringExpression1 realmId() default @StringExpression1;

  StringExpression1 createUserId() default @StringExpression1;

  StringExpression1 updateUserId() default @StringExpression1;

  StringExpression1 createGroupId() default @StringExpression1;

  StringExpression1 __typename() default @StringExpression1;

  StringExpression1 typeNameMax() default @StringExpression1;

  StringExpression1 typeNameMin() default @StringExpression1;

  StringExpression1 possibleTypeNameMax() default @StringExpression1;

  StringExpression1 possibleTypeNameMin() default @StringExpression1;

  __TypePossibleTypesExpression1[] exs() default {};
}
