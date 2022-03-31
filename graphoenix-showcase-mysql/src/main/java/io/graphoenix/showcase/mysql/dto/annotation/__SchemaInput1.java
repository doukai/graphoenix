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

  __TypeInput2[] types() default {};

  __TypeInput2 queryType() default @__TypeInput2;

  __TypeInput2 mutationType() default @__TypeInput2;

  __TypeInput2 subscriptionType() default @__TypeInput2;

  __DirectiveInput2[] directives() default {};

  __TypeInput2 typesAggregate() default @__TypeInput2;

  __DirectiveInput2 directivesAggregate() default @__DirectiveInput2;

  __TypeConnectionInput2 typesConnection() default @__TypeConnectionInput2;

  __DirectiveConnectionInput2 directivesConnection() default @__DirectiveConnectionInput2;
}
