package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.TypeExpressions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpressions
public @interface __SchemaExpressions {
  Conditional cond() default Conditional.AND;

  __SchemaExpression[] value() default {};

  __TypeExpression[] types() default {};

  __DirectiveExpression[] directives() default {};

  __TypeExpression[] subscriptionType() default {};

  __TypeExpression[] mutationType() default {};

  __TypeExpression[] queryType() default {};
}
