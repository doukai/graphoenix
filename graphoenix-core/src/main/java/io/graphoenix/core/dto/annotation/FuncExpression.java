package io.graphoenix.core.dto.annotation;

import io.graphoenix.core.dto.enumType.Func;
import io.graphoenix.core.dto.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface FuncExpression {
  Operator opr() default Operator.EQ;

  Func val() default Func.COUNT;

  Func[] in() default {};

  String $val() default "";

  String $in() default "";
}
