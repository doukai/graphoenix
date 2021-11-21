package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserExpression {
  Operator opr() default Operator.EQ;

  String organizationId() default "";

  String password() default "";

  String isDeprecated() default "";

  String disable() default "";

  String sex() default "";

  String name() default "";

  String id() default "";

  String login() default "";

  String version() default "";

  String age() default "";
}
