package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleExpression {
  Operator opr() default Operator.EQ;

  boolean isDeprecated() default false;

  String name() default "";

  int id() default 0;

  int version() default 0;

  String $isDeprecated() default "";

  String $name() default "";

  String $id() default "";

  String $version() default "";
}
