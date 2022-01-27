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
public @interface __SchemaInput {
  boolean isDeprecated() default false;

  __TypeInnerInput[] types() default {};

  __DirectiveInnerInput[] directives() default {};

  String mutationTypeName() default "";

  __TypeInnerInput subscriptionType() default @__TypeInnerInput;

  __TypeInnerInput mutationType() default @__TypeInnerInput;

  String subscriptionTypeName() default "";

  String queryTypeName() default "";

  String id() default "";

  int version() default 0;

  __TypeInnerInput queryType() default @__TypeInnerInput;

  String $isDeprecated() default "";

  String $types() default "";

  String $directives() default "";

  String $mutationTypeName() default "";

  String $subscriptionType() default "";

  String $mutationType() default "";

  String $subscriptionTypeName() default "";

  String $queryTypeName() default "";

  String $id() default "";

  String $version() default "";

  String $queryType() default "";
}
