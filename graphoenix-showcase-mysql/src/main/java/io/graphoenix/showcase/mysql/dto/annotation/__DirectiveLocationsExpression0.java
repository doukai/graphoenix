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
public @interface __DirectiveLocationsExpression0 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  String[] directiveName() default {};

  __DirectiveLocation[] directiveLocation() default {};

  int[] version() default {};

  boolean[] isDeprecated() default {};

  String[] __typename() default {};

  String[] $id() default {};

  String[] $directiveName() default {};

  String[] $directiveLocation() default {};

  String[] $version() default {};

  String[] $isDeprecated() default {};

  String[] $__typename() default {};
}