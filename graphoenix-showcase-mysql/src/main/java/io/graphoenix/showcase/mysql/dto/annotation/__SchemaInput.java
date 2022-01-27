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
  __TypeInnerInput[] types() default {};

  boolean isDeprecated() default false;

  String mutationTypeName() default "";

  __TypeInnerInput subscriptionType() default @__TypeInnerInput;

  __DirectiveInnerInput[] directives() default {};

  __TypeInnerInput mutationType() default @__TypeInnerInput;

  String subscriptionTypeName() default "";

  String queryTypeName() default "";

  String id() default "";

  int version() default 0;

  __TypeInnerInput queryType() default @__TypeInnerInput;

  String $types() default "";

  String $isDeprecated() default "";

  String $mutationTypeName() default "";

  String $subscriptionType() default "";

  String $directives() default "";

  String $mutationType() default "";

  String $subscriptionTypeName() default "";

  String $queryTypeName() default "";

  String $id() default "";

  String $version() default "";

  String $queryType() default "";
}
