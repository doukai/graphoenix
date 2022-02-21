package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface OrganizationExpression0 {
  Operator opr() default Operator.EQ;

  boolean[] isDeprecated() default {};

  String[] __typename() default {};

  boolean[] roleDisable() default {};

  int[] version() default {};

  int[] aboveId() default {};

  String[] name() default {};

  int[] orgLevel2() default {};

  int[] orgLevel3() default {};

  String[] id() default {};

  String[] $isDeprecated() default {};

  String[] $__typename() default {};

  String[] $roleDisable() default {};

  String[] $version() default {};

  String[] $aboveId() default {};

  String[] $name() default {};

  String[] $orgLevel2() default {};

  String[] $orgLevel3() default {};

  String[] $id() default {};

  OrganizationExpressions1[] parent() default {};

  UserExpressions1[] userByOrg() default {};

  UserExpressions1[] users() default {};

  OrganizationExpressions1[] above() default {};
}
