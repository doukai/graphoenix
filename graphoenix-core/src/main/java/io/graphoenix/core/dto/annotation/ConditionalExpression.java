package io.graphoenix.core.dto.annotation;

import io.graphoenix.core.dto.enumType.Conditional;
import io.graphoenix.core.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface ConditionalExpression {
  Operator opr() default Operator.EQ;

  Conditional val() default Conditional.AND;

  Conditional[] in() default {};

  String $val() default "";

  String $in() default "";
}
