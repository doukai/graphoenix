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
public @interface RoleEdgeExpressions0 {
  Conditional cond() default Conditional.AND;

  RoleEdgeExpression0[] value() default {};
}
