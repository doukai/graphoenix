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
public @interface __EnumValueInput2 {
  String deprecationReason() default "";

  boolean isDeprecated() default false;

  String __typename() default "";

  String name() default "";

  String description() default "";

  String id() default "";

  int version() default 0;

  String ofTypeName() default "";

  String $deprecationReason() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  String $name() default "";

  String $description() default "";

  String $id() default "";

  String $version() default "";

  String $ofTypeName() default "";
}
