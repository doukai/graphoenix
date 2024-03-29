package io.graphoenix.structure.dto.annotation;

import io.graphoenix.core.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface StringExpression {
  Operator opr() default Operator.EQ;

  String val() default "";

  String[] in() default {};

  String $val() default "";

  String $in() default "";

  boolean skipNull() default false;
}
