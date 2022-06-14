package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface BooleanExpression {
  Operator opr() default Operator.EQ;

  boolean val() default false;

  boolean[] in() default {};

  String $val() default "";

  String $in() default "";

  boolean skipNull() default false;
}
