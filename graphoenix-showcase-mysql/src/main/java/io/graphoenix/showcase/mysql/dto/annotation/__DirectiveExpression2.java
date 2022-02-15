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
public @interface __DirectiveExpression2 {
  Operator opr() default Operator.EQ;

  boolean[] onFragment() default {};

  boolean[] isDeprecated() default {};

  int[] schemaId() default {};

  String[] name() default {};

  String[] description() default {};

  __DirectiveLocation[] locations() default {};

  boolean[] onOperation() default {};

  int[] version() default {};

  boolean[] onField() default {};

  String[] $onFragment() default {};

  String[] $isDeprecated() default {};

  String[] $schemaId() default {};

  String[] $name() default {};

  String[] $description() default {};

  String[] $locations() default {};

  String[] $onOperation() default {};

  String[] $version() default {};

  String[] $onField() default {};
}
