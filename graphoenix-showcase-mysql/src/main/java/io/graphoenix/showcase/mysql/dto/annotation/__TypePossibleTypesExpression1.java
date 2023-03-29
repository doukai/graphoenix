package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypePossibleTypesExpression1 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression typeName() default @StringExpression;

  StringExpression possibleTypeName() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  String[] groupBy() default {};

  __TypePossibleTypesOrderBy1 orderBy() default @__TypePossibleTypesOrderBy1;

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

  __TypePossibleTypesExpression2[] exs() default {};
}
