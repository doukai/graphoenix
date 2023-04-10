package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.showcase.user.dto.enumType.Operator;
import io.graphoenix.showcase.user.dto.enumType.__TypeKind;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeKindExpression {
  Operator opr() default Operator.EQ;

  __TypeKind val() default __TypeKind.SCALAR;

  __TypeKind[] in() default {};

  String $val() default "";

  String $in() default "";
}
