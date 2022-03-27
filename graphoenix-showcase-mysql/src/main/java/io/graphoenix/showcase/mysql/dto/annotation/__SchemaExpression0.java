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
public @interface __SchemaExpression0 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  String[] queryTypeName() default {};

  String[] mutationTypeName() default {};

  String[] subscriptionTypeName() default {};

  int[] version() default {};

  boolean[] isDeprecated() default {};

  String[] __typename() default {};

  String[] $id() default {};

  String[] $queryTypeName() default {};

  String[] $mutationTypeName() default {};

  String[] $subscriptionTypeName() default {};

  String[] $version() default {};

  String[] $isDeprecated() default {};

  String[] $__typename() default {};

  __TypeExpressions1[] types() default {};

  __TypeExpressions1[] queryType() default {};

  __TypeExpressions1[] mutationType() default {};

  __TypeExpressions1[] subscriptionType() default {};

  __DirectiveExpressions1[] directives() default {};

  __TypeExpressions1[] typesAggregate() default {};

  __DirectiveExpressions1[] directivesAggregate() default {};
}
