package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __SchemaInput1 {
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

  StringInput2 queryTypeName() default "";

  StringInput2 mutationTypeName() default "";

  StringInput2 subscriptionTypeName() default "";

  __TypeInput2[] types() default {};

  __TypeInput2 queryType() default @__TypeInput2;

  __TypeInput2 mutationType() default @__TypeInput2;

  __TypeInput2 subscriptionType() default @__TypeInput2;

  __DirectiveInput2[] directives() default {};

  StringInput2 realmId() default "";

  StringInput2 createUserId() default "";

  StringInput2 updateUserId() default "";

  StringInput2 createGroupId() default "";

  StringInput2 __typename() default "";

  StringInput2 queryTypeNameMax() default "";

  StringInput2 queryTypeNameMin() default "";

  StringInput2 mutationTypeNameMax() default "";

  StringInput2 mutationTypeNameMin() default "";

  StringInput2 subscriptionTypeNameMax() default "";

  StringInput2 subscriptionTypeNameMin() default "";

  __SchemaInput2[] list() default {};

  String $list() default "";
}
