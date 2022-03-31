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
  boolean hasNextPage() default false;

  boolean hasPreviousPage() default false;

  String startCursor() default "";

  String endCursor() default "";

  String $hasNextPage() default "";

  String $hasPreviousPage() default "";

  String $startCursor() default "";

  String $endCursor() default "";
}
