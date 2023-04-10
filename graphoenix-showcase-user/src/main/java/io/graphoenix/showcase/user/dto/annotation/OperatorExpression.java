package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.showcase.user.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OperatorExpression {
  Operator opr() default Operator.EQ;

  Operator val() default Operator.EQ;

  Operator[] in() default {};

  String $val() default "";

  String $in() default "";
}
