package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import io.graphoenix.spi.annotation.TypeExpressions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpressions
public @interface UserExpressions {
  Conditional cond() default Conditional.AND;

  UserExpression[] value() default {};

  OrganizationExpression[] organization() default {};

  RoleExpression[] roles() default {};
}
