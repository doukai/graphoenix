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
public @interface PageInfoInput0 {
  boolean isDeprecated() default false;

  boolean hasNextPage() default false;

  String __typename() default "";

  boolean hasPreviousPage() default false;

  String endCursor() default "";

  String startCursor() default "";

  int version() default 0;

  String $isDeprecated() default "";

  String $hasNextPage() default "";

  String $__typename() default "";

  String $hasPreviousPage() default "";

  String $endCursor() default "";

  String $startCursor() default "";

  String $version() default "";
}
