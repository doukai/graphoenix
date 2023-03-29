package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeInterfacesOrderBy1 {
  Sort id() default Sort.ASC;

  Sort typeName() default Sort.ASC;

  Sort interfaceName() default Sort.ASC;

  Sort __typename() default Sort.ASC;
}
