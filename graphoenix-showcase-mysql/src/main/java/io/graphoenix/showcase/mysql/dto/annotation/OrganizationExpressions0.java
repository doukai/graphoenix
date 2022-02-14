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
public @interface OrganizationExpressions0 {
  Conditional cond() default Conditional.AND;

  OrganizationExpression0[] value() default {};
}
