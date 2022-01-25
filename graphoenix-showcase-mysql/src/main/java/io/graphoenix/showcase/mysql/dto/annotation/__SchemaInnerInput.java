package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __SchemaInnerInput {
  boolean isDeprecated() default false;

  String mutationTypeName() default "";

  String subscriptionTypeName() default "";

  String queryTypeName() default "";

  int id() default 0;

  int version() default 0;

  String $isDeprecated() default "";

  String $mutationTypeName() default "";

  String $subscriptionTypeName() default "";

  String $queryTypeName() default "";

  String $id() default "";

  String $version() default "";
}
