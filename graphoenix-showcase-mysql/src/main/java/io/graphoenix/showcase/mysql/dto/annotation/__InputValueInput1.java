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
public @interface __InputValueInput1 {
  String directiveName() default "";

  boolean isDeprecated() default false;

  String defaultValue() default "";

  String __typename() default "";

  String name() default "";

  String typeName() default "";

  String description() default "";

  String id() default "";

  int version() default 0;

  String ofTypeName() default "";

  int fieldId() default 0;

  String $directiveName() default "";

  String $isDeprecated() default "";

  String $defaultValue() default "";

  String $__typename() default "";

  String $name() default "";

  String $typeName() default "";

  String $description() default "";

  String $id() default "";

  String $type() default "";

  String $version() default "";

  String $ofTypeName() default "";

  String $fieldId() default "";

  __TypeInput2 type() default @__TypeInput2;
}
