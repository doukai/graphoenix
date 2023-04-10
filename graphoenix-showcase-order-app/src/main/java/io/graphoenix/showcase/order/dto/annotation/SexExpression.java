package io.graphoenix.showcase.order.dto.annotation;

import io.graphoenix.showcase.order.dto.enumType.Operator;
import io.graphoenix.showcase.order.dto.enumType.Sex;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface SexExpression {
  Operator opr() default Operator.EQ;

  Sex val() default Sex.MAN;

  Sex[] in() default {};

  String $val() default "";

  String $in() default "";
}
