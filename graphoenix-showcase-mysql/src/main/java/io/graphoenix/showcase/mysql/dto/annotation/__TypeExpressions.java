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
public @interface __TypeExpressions {
  Conditional cond() default Conditional.AND;

  __TypeExpression[] value() default {};

  __TypeExpression[] interfaces() default {};

  __TypeExpression[] possibleTypes() default {};

  __InputValueExpression[] inputFields() default {};

  __FieldExpression[] fields() default {};

  __TypeExpression[] ofType() default {};

  __EnumValueExpression[] enumValues() default {};
}
