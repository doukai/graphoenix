package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __TypeInnerInput {
  boolean isDeprecated() default false;

  __TypeKind kind() default __TypeKind.SCALAR;

  String description() default "";

  int version() default 0;

  int schemaId() default 0;

  int name() default 0;

  String ofTypeName() default "";

  String $isDeprecated() default "";

  String $kind() default "";

  String $description() default "";

  String $version() default "";

  String $schemaId() default "";

  String $name() default "";

  String $ofTypeName() default "";
}
