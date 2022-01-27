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
public @interface __InputValueExpression {
  Operator opr() default Operator.EQ;

  String[] directiveName() default {};

  boolean[] isDeprecated() default {};

  String[] defaultValue() default {};

  String[] typeName() default {};

  String[] name() default {};

  String[] description() default {};

  String[] id() default {};

  int[] version() default {};

  String[] ofTypeName() default {};

  int[] fieldId() default {};

  String[] $directiveName() default {};

  String[] $isDeprecated() default {};

  String[] $defaultValue() default {};

  String[] $typeName() default {};

  String[] $name() default {};

  String[] $description() default {};

  String[] $id() default {};

  String[] $version() default {};

  String[] $ofTypeName() default {};

  String[] $fieldId() default {};
}
