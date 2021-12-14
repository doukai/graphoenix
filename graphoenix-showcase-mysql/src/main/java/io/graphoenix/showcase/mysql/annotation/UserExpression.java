package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.showcase.mysql.enumType.Operator;
import io.graphoenix.showcase.mysql.enumType.Sex;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface UserExpression {
  Operator opr() default Operator.EQ;

  int[] organizationId() default {};

  String[] password() default {};

  boolean[] disable() default {};

  Sex[] sex() default {};

  String[] name() default {};

  String[] phones() default {};

  int[] id() default {};

  String[] login() default {};

  int[] age() default {};

  String[] $organizationId() default {};

  String[] $password() default {};

  String[] $disable() default {};

  String[] $sex() default {};

  String[] $name() default {};

  String[] $phones() default {};

  String[] $id() default {};

  String[] $login() default {};

  String[] $age() default {};
}
