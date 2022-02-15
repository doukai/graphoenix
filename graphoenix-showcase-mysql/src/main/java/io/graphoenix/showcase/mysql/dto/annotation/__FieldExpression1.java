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
public @interface __FieldExpression1 {
  Operator opr() default Operator.EQ;

  String[] deprecationReason() default {};

  boolean[] isDeprecated() default {};

  String[] name() default {};

  String[] typeName() default {};

  String[] description() default {};

  String[] id() default {};

  int[] version() default {};

  String[] ofTypeName() default {};

  String[] $deprecationReason() default {};

  String[] $isDeprecated() default {};

  String[] $name() default {};

  String[] $typeName() default {};

  String[] $description() default {};

  String[] $id() default {};

  String[] $version() default {};

  String[] $ofTypeName() default {};

  __InputValueExpressions2[] args() default {};

  __TypeExpressions2[] type() default {};
}
