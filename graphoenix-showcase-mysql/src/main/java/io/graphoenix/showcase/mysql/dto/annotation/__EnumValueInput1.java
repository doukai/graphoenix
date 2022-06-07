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
public @interface __EnumValueInput1 {
  String id() default "";

  String name() default "";

  String ofTypeName() default "";

  String description() default "";

  String deprecationReason() default "";

  int version() default 0;

  boolean isDeprecated() default false;

  String __typename() default "";

  String $id() default "";

  String $name() default "";

  String $ofTypeName() default "";

  String $ofType() default "";

  String $description() default "";

  String $deprecationReason() default "";

  String $version() default "";

  String $isDeprecated() default "";

  String $__typename() default "";

  __TypeInput2 ofType() default @__TypeInput2;
}
