package io.graphoenix.showcase.mysql.generated.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserInputs {
  UserInput value() default @UserInput;

  OrganizationInput organization() default @OrganizationInput;

  RoleInput[] roles() default {};
}
