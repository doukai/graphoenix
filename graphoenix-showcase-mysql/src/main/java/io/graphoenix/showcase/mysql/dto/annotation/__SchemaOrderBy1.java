package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __SchemaOrderBy1 {
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

  StringOrderBy2 queryTypeName() default @StringOrderBy2;

  StringOrderBy2 mutationTypeName() default @StringOrderBy2;

  StringOrderBy2 subscriptionTypeName() default @StringOrderBy2;

  __TypeOrderBy2 types() default @__TypeOrderBy2;

  __TypeOrderBy2 queryType() default @__TypeOrderBy2;

  __TypeOrderBy2 mutationType() default @__TypeOrderBy2;

  __TypeOrderBy2 subscriptionType() default @__TypeOrderBy2;

  __DirectiveOrderBy2 directives() default @__DirectiveOrderBy2;

  StringOrderBy2 realmId() default @StringOrderBy2;

  StringOrderBy2 createUserId() default @StringOrderBy2;

  StringOrderBy2 updateUserId() default @StringOrderBy2;

  StringOrderBy2 createGroupId() default @StringOrderBy2;

  StringOrderBy2 __typename() default @StringOrderBy2;

  StringOrderBy2 queryTypeNameMax() default @StringOrderBy2;

  StringOrderBy2 queryTypeNameMin() default @StringOrderBy2;

  StringOrderBy2 mutationTypeNameMax() default @StringOrderBy2;

  StringOrderBy2 mutationTypeNameMin() default @StringOrderBy2;

  StringOrderBy2 subscriptionTypeNameMax() default @StringOrderBy2;

  StringOrderBy2 subscriptionTypeNameMin() default @StringOrderBy2;
}
