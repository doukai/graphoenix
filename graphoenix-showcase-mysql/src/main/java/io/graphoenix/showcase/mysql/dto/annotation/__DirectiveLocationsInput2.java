package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveLocationsInput2 {
  String id() default "";

  String directiveName() default "";

  __DirectiveLocation directiveLocation() default __DirectiveLocation.QUERY;

  String __typename() default "";

  String $id() default "";

  String $directiveName() default "";

  String $directiveLocation() default "";

  String $__typename() default "";
}
