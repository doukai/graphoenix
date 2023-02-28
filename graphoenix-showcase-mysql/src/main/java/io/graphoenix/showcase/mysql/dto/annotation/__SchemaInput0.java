package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __SchemaInput0 {
  String id() default "";

  String queryTypeName() default "";

  String mutationTypeName() default "";

  String subscriptionTypeName() default "";

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

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

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $typesAggregate() default "";

  String $directivesAggregate() default "";

  String $typesConnection() default "";

  String $directivesConnection() default "";

  StringInput1 queryTypeName() default "";

  StringInput1 mutationTypeName() default "";

  StringInput1 subscriptionTypeName() default "";

  __TypeInput1[] types() default {};

  __TypeInput1 queryType() default @__TypeInput1;

  __TypeInput1 mutationType() default @__TypeInput1;

  __TypeInput1 subscriptionType() default @__TypeInput1;

  __DirectiveInput1[] directives() default {};

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  StringInput1 queryTypeNameMax() default "";

  StringInput1 queryTypeNameMin() default "";

  StringInput1 mutationTypeNameMax() default "";

  StringInput1 mutationTypeNameMin() default "";

  StringInput1 subscriptionTypeNameMax() default "";

  StringInput1 subscriptionTypeNameMin() default "";

  __SchemaInput1[] list() default {};

  String $list() default "";
}
