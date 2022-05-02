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
public @interface UserTest2ConnectionExpression0 {
  Operator opr() default Operator.EQ;

  int[] totalCount() default {};

  String[] $totalCount() default {};

  PageInfoExpressions1[] pageInfo() default {};

  UserTest2EdgeExpressions1[] edges() default {};
}
