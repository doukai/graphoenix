package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.core.dto.enumType.Operator;
import io.graphoenix.showcase.user.dto.enumType.Sex;
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
