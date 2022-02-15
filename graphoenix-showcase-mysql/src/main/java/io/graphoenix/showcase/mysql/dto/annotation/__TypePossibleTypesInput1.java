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
public @interface __TypePossibleTypesInput1 {
  boolean isDeprecated() default false;

  String typeName() default "";

  String possibleTypeName() default "";

  String id() default "";

  int version() default 0;

  String $isDeprecated() default "";

  String $typeName() default "";

  String $possibleTypeName() default "";

  String $id() default "";

  String $version() default "";
}
