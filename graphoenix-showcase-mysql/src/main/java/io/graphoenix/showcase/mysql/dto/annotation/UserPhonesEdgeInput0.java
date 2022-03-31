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
public @interface UserPhonesEdgeInput0 {
  String cursor() default "";

  String $node() default "";

  String $cursor() default "";

  UserPhonesInput1 node() default @UserPhonesInput1;
}
