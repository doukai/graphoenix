package io.graphoenix.showcase.order.dto.annotation;

import io.graphoenix.core.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface FloatExpression {
  Operator opr() default Operator.EQ;

  float val() default 0;

  float[] in() default {};

  String $val() default "";

  String $in() default "";

  boolean skipNull() default false;
}
