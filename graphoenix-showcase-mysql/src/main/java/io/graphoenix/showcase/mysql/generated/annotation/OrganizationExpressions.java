package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.showcase.mysql.generated.enumType.Conditional;
import io.graphoenix.spi.annotation.TypeExpressions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpressions
public @interface OrganizationExpressions {
  Conditional cond() default Conditional.AND;

  OrganizationExpression[] value() default {};

  OrganizationExpression[] above() default {};

  UserExpression[] users() default {};
}
