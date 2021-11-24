package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserRoleExpression {
  Operator opr() default Operator.EQ;

  boolean isDeprecated() default false;

  int roleId() default 0;

  int id() default 0;

  int userId() default 0;

  int version() default 0;

  String $isDeprecated() default "";

  String $roleId() default "";

  String $id() default "";

  String $userId() default "";

  String $version() default "";
}
