package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __InputValueOrderBy1 {
  Sort id() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort typeName() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort fieldId() default Sort.ASC;

  Sort directiveName() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort defaultValue() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  __TypeOrderBy2 ofType() default @__TypeOrderBy2;

  __TypeOrderBy2 type() default @__TypeOrderBy2;
}
