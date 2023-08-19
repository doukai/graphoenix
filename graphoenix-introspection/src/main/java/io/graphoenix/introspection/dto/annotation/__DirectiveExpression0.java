package io.graphoenix.introspection.dto.annotation;

import io.graphoenix.core.dto.annotation.__DirectiveLocationExpression;
import io.graphoenix.core.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression name() default @IDExpression;

  StringExpression description() default @StringExpression;

  __DirectiveLocationExpression locations() default @__DirectiveLocationExpression;

  BooleanExpression isRepeatable() default @BooleanExpression;

  BooleanExpression isDeprecated() default @BooleanExpression;

  IntExpression version() default @IntExpression;

  IntExpression realmId() default @IntExpression;

  StringExpression createUserId() default @StringExpression;

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  IntExpression schemaId() default @IntExpression;

  String[] groupBy() default {};

  __DirectiveOrderBy0 orderBy() default @__DirectiveOrderBy0;

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
