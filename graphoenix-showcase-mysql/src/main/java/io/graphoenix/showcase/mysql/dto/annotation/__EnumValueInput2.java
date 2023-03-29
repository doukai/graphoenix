package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __EnumValueInput2 {
  String id() default "";

  String name() default "";

  String ofTypeName() default "";

  String description() default "";

  String deprecationReason() default "";

  String __typename() default "";

  String $id() default "";

  String $name() default "";

  String $ofTypeName() default "";

  String $ofType() default "";

  String $description() default "";

  String $deprecationReason() default "";

  String $__typename() default "";
}
