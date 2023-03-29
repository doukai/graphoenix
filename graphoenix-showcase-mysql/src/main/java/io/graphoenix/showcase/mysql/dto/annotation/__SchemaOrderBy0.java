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

  Sort __typename() default Sort.ASC;

  __TypeOrderBy1 types() default @__TypeOrderBy1;

  __TypeOrderBy1 queryType() default @__TypeOrderBy1;

  __TypeOrderBy1 mutationType() default @__TypeOrderBy1;

  __TypeOrderBy1 subscriptionType() default @__TypeOrderBy1;

  __DirectiveOrderBy1 directives() default @__DirectiveOrderBy1;
}
