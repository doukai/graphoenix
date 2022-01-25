package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface __TypeExpression {
  Operator opr() default Operator.EQ;

  boolean[] isDeprecated() default {};

  __TypeKind[] kind() default {};

  String[] description() default {};

  int[] version() default {};

  int[] schemaId() default {};

  String[] name() default {};

  String[] ofTypeName() default {};

  String[] $isDeprecated() default {};

  String[] $kind() default {};

  String[] $description() default {};

  String[] $version() default {};

  String[] $schemaId() default {};

  String[] $name() default {};

  String[] $ofTypeName() default {};
}
