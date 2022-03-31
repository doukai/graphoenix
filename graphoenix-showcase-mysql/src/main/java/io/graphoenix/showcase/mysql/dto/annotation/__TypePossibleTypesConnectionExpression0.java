package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface __TypePossibleTypesConnectionExpression0 {
  Operator opr() default Operator.EQ;

  PageInfoExpressions1[] pageInfo() default {};

  __TypePossibleTypesEdgeExpressions1[] edges() default {};
}
