package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression name() default @StringExpression;

  StringExpression typeName() default @StringExpression;

  StringExpression ofTypeName() default @StringExpression;

  StringExpression description() default @StringExpression;

  StringExpression deprecationReason() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  String[] groupBy() default {};

  __FieldOrderBy0 orderBy() default @__FieldOrderBy0;

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

  __TypeExpression1 ofType() default @__TypeExpression1;

  __InputValueExpression1 args() default @__InputValueExpression1;

  __TypeExpression1 type() default @__TypeExpression1;

  __FieldExpression1[] exs() default {};
}
