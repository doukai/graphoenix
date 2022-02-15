package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface UserExpression2 {
  Operator opr() default Operator.EQ;

  boolean[] isDeprecated() default {};

  Sex[] sex() default {};

  String[] phones() default {};

  String[] login() default {};

  String[] userDetail2() default {};

  int[] version() default {};

  int[] organizationId() default {};

  String[] password() default {};

  boolean[] disable() default {};

  String[] name() default {};

  String[] id() default {};

  int[] age() default {};

  String[] $isDeprecated() default {};

  String[] $sex() default {};

  String[] $phones() default {};

  String[] $login() default {};

  String[] $userDetail2() default {};

  String[] $version() default {};

  String[] $organizationId() default {};

  String[] $password() default {};

  String[] $disable() default {};

  String[] $name() default {};

  String[] $id() default {};

  String[] $age() default {};
}
