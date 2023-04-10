package io.graphoenix.showcase.order.dto.annotation;

import io.graphoenix.showcase.order.dto.enumType.Operator;
import io.graphoenix.showcase.order.dto.enumType.__DirectiveLocation;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveLocationExpression {
  Operator opr() default Operator.EQ;

  __DirectiveLocation val() default __DirectiveLocation.QUERY;

  __DirectiveLocation[] in() default {};

  String $val() default "";

  String $in() default "";
}
