package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __DirectiveLocationsInput2 {
  String directiveName() default "";

  boolean isDeprecated() default false;

  __DirectiveLocation directiveLocation() default __DirectiveLocation.QUERY;

  String id() default "";

  int version() default 0;

  String $directiveName() default "";

  String $isDeprecated() default "";

  String $directiveLocation() default "";

  String $id() default "";

  String $version() default "";
}
