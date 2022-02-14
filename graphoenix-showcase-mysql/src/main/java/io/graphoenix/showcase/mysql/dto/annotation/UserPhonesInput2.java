package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Name("UserPhonesInput")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface UserPhonesInput2 {
  boolean isDeprecated() default false;

  String phone() default "";

  String id() default "";

  int userId() default 0;

  int version() default 0;

  String $isDeprecated() default "";

  String $phone() default "";

  String $id() default "";

  String $userId() default "";

  String $version() default "";
}
