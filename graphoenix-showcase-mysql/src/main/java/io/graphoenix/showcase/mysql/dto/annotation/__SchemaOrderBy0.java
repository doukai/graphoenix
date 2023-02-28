package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
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

  StringOrderBy1 queryTypeName() default @StringOrderBy1;

  StringOrderBy1 mutationTypeName() default @StringOrderBy1;

  StringOrderBy1 subscriptionTypeName() default @StringOrderBy1;

  __TypeOrderBy1 types() default @__TypeOrderBy1;

  __TypeOrderBy1 queryType() default @__TypeOrderBy1;

  __TypeOrderBy1 mutationType() default @__TypeOrderBy1;

  __TypeOrderBy1 subscriptionType() default @__TypeOrderBy1;

  __DirectiveOrderBy1 directives() default @__DirectiveOrderBy1;

  StringOrderBy1 realmId() default @StringOrderBy1;

  StringOrderBy1 createUserId() default @StringOrderBy1;

  StringOrderBy1 updateUserId() default @StringOrderBy1;

  StringOrderBy1 createGroupId() default @StringOrderBy1;

  StringOrderBy1 __typename() default @StringOrderBy1;

  StringOrderBy1 queryTypeNameMax() default @StringOrderBy1;

  StringOrderBy1 queryTypeNameMin() default @StringOrderBy1;

  StringOrderBy1 mutationTypeNameMax() default @StringOrderBy1;

  StringOrderBy1 mutationTypeNameMin() default @StringOrderBy1;

  StringOrderBy1 subscriptionTypeNameMax() default @StringOrderBy1;

  StringOrderBy1 subscriptionTypeNameMin() default @StringOrderBy1;
}
