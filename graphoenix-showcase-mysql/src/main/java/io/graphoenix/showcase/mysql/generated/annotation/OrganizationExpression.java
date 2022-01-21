package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface OrganizationExpression {
  Operator opr() default Operator.EQ;

  int[] aboveId() default {};

  boolean[] isDeprecated() default {};

  String[] name() default {};

  int[] orgLevel2() default {};

  int[] orgLevel3() default {};

  boolean[] roleDisable() default {};

  int[] id() default {};

  int[] version() default {};

  String[] $aboveId() default {};

  String[] $isDeprecated() default {};

  String[] $name() default {};

  String[] $orgLevel2() default {};

  String[] $orgLevel3() default {};

  String[] $roleDisable() default {};

  String[] $id() default {};

  String[] $version() default {};
}
