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
public @interface __SchemaInput1 {
  boolean isDeprecated() default false;

  String mutationTypeName() default "";

  String subscriptionTypeName() default "";

  String __typename() default "";

  String queryTypeName() default "";

  String id() default "";

  int version() default 0;

  String $types() default "";

  String $isDeprecated() default "";

  String $mutationTypeName() default "";

  String $subscriptionType() default "";

  String $directives() default "";

  String $mutationType() default "";

  String $subscriptionTypeName() default "";

  String $__typename() default "";

  String $queryTypeName() default "";

  String $id() default "";

  String $version() default "";

  String $queryType() default "";

  __TypeInput2[] types() default {};

  __TypeInput2 subscriptionType() default @__TypeInput2;

  __DirectiveInput2[] directives() default {};

  __TypeInput2 mutationType() default @__TypeInput2;

  __TypeInput2 queryType() default @__TypeInput2;
}
