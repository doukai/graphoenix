package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface __DirectiveExpression1 {
  Operator opr() default Operator.EQ;

  String[] name() default {};

  int[] schemaId() default {};

  String[] description() default {};

  __DirectiveLocation[] locations() default {};

  boolean[] onOperation() default {};

  boolean[] onFragment() default {};

  boolean[] onField() default {};

  int[] version() default {};

  boolean[] isDeprecated() default {};

  String[] __typename() default {};

  String[] $name() default {};

  String[] $schemaId() default {};

  String[] $description() default {};

  String[] $locations() default {};

  String[] $onOperation() default {};

  String[] $onFragment() default {};

  String[] $onField() default {};

  String[] $version() default {};

  String[] $isDeprecated() default {};

  String[] $__typename() default {};

  __InputValueExpressions2[] args() default {};
}
