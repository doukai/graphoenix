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
public @interface __EnumValueConnectionInput0 {
  String $pageInfo() default "";

  String $edges() default "";

  PageInfoInput1 pageInfo() default @PageInfoInput1;

  __EnumValueEdgeInput1[] edges() default {};
}
