package io.graphoenix.java.generator.directive;

import io.graphoenix.java.generator.enumType.Conditional;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleExpressions {
  Conditional cond() default Conditional.AND;

  RoleExpression[] value() default {};

  UserExpression[] users() default {};
}
