package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveOrderBy2 {
  Sort name() default Sort.ASC;

  Sort schemaId() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort locations() default Sort.ASC;

  Sort onOperation() default Sort.ASC;

  Sort onFragment() default Sort.ASC;

  Sort onField() default Sort.ASC;

  Sort __typename() default Sort.ASC;
}
