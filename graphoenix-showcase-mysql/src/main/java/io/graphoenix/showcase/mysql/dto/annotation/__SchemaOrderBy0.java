package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __SchemaOrderBy0 {
  Sort id() default Sort.ASC;

  Sort queryTypeName() default Sort.ASC;

  Sort mutationTypeName() default Sort.ASC;

  Sort subscriptionTypeName() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

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

  __TypeOrderBy1 types() default @__TypeOrderBy1;

  __TypeOrderBy1 queryType() default @__TypeOrderBy1;

  __TypeOrderBy1 mutationType() default @__TypeOrderBy1;

  __TypeOrderBy1 subscriptionType() default @__TypeOrderBy1;

  __DirectiveOrderBy1 directives() default @__DirectiveOrderBy1;
}
