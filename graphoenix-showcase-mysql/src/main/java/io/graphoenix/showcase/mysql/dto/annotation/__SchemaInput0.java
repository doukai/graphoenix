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
public @interface __SchemaInput0 {
  String id() default "";

  String queryTypeName() default "";

  String mutationTypeName() default "";

  String subscriptionTypeName() default "";

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  String $id() default "";

  String $queryTypeName() default "";

  String $mutationTypeName() default "";

  String $subscriptionTypeName() default "";

  String $types() default "";

  String $queryType() default "";

  String $mutationType() default "";

  String $subscriptionType() default "";

  String $directives() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $typesAggregate() default "";

  String $directivesAggregate() default "";

  String $typesConnection() default "";

  String $directivesConnection() default "";

  __TypeInput1[] types() default {};

  __TypeInput1 queryType() default @__TypeInput1;

  __TypeInput1 mutationType() default @__TypeInput1;

  __TypeInput1 subscriptionType() default @__TypeInput1;

  __DirectiveInput1[] directives() default {};

  __TypeInput1 typesAggregate() default @__TypeInput1;

  __DirectiveInput1 directivesAggregate() default @__DirectiveInput1;

  __TypeConnectionInput1 typesConnection() default @__TypeConnectionInput1;

  __DirectiveConnectionInput1 directivesConnection() default @__DirectiveConnectionInput1;
}
