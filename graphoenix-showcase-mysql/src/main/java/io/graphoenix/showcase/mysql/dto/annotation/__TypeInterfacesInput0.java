package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeInterfacesInput0 {
  String id() default "";

  String typeName() default "";

  String interfaceName() default "";

  String __typename() default "";

  String $id() default "";

  String $typeName() default "";

  String $interfaceName() default "";

  String $__typename() default "";

  __TypeInterfacesInput1[] list() default {};

  String $list() default "";
}
