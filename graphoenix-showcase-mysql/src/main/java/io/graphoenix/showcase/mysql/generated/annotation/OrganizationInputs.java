package io.graphoenix.showcase.mysql.generated.annotation;

import io.graphoenix.spi.annotation.TypeInputs;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInputs
public @interface OrganizationInputs {
  OrganizationInput[] value() default {};

  OrganizationInput[] above() default {};

  UserInput[] users() default {};
}
