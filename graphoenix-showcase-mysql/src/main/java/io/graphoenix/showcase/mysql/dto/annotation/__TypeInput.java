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
public @interface __TypeInput {
  __TypeInnerInput[] interfaces() default {};

  boolean isDeprecated() default false;

  __TypeInnerInput[] possibleTypes() default {};

  __TypeKind kind() default __TypeKind.SCALAR;

  String description() default "";

  int version() default 0;

  __InputValueInnerInput[] inputFields() default {};

  int schemaId() default 0;

  int name() default 0;

  __FieldInnerInput[] fields() default {};

  __TypeInnerInput ofType() default @__TypeInnerInput;

  __EnumValueInnerInput[] enumValues() default {};

  String ofTypeName() default "";

  String $interfaces() default "";

  String $isDeprecated() default "";

  String $possibleTypes() default "";

  String $kind() default "";

  String $description() default "";

  String $version() default "";

  String $inputFields() default "";

  String $schemaId() default "";

  String $name() default "";

  String $fields() default "";

  String $ofType() default "";

  String $enumValues() default "";

  String $ofTypeName() default "";
}
