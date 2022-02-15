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
public @interface __TypeExpression0 {
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

  __TypeExpressions1[] interfaces() default {};

  __TypeExpressions1[] possibleTypes() default {};

  __InputValueExpressions1[] inputFields() default {};

  __FieldExpressions1[] fields() default {};

  __TypeExpressions1[] ofType() default {};

  __EnumValueExpressions1[] enumValues() default {};
}
