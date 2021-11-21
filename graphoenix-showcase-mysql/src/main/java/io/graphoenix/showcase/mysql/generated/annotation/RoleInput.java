package io.graphoenix.showcase.mysql.generated.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RoleInput {
  String isDeprecated() default "";

  String name() default "";

  String id() default "";

  String version() default "";
}
