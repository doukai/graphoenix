package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface PageInfoExpression2 {
  Operator opr() default Operator.EQ;

  boolean[] isDeprecated() default {};

  boolean[] hasNextPage() default {};

  String[] __typename() default {};

  boolean[] hasPreviousPage() default {};

  String[] endCursor() default {};

  String[] startCursor() default {};

  int[] version() default {};

  String[] $isDeprecated() default {};

  String[] $hasNextPage() default {};

  String[] $__typename() default {};

  String[] $hasPreviousPage() default {};

  String[] $endCursor() default {};

  String[] $startCursor() default {};

  String[] $version() default {};
}
