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
public @interface __FieldInput1 {
  String deprecationReason() default "";

  boolean isDeprecated() default false;

  String name() default "";

  String typeName() default "";

  String description() default "";

  String id() default "";

  int version() default 0;

  String ofTypeName() default "";

  String $args() default "";

  String $deprecationReason() default "";

  String $isDeprecated() default "";

  String $name() default "";

  String $typeName() default "";

  String $description() default "";

  String $id() default "";

  String $type() default "";

  String $version() default "";

  String $ofTypeName() default "";

  __InputValueInput2[] args() default {};

  __TypeInput2 type() default @__TypeInput2;
}
