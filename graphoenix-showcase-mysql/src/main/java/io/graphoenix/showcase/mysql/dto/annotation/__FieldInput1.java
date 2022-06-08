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
public @interface __FieldInput1 {
  String id() default "";

  String name() default "";

  String typeName() default "";

  String ofTypeName() default "";

  String description() default "";

  String deprecationReason() default "";

  String from() default "";

  String to() default "";

  String withType() default "";

  String withFrom() default "";

  String withTo() default "";

  String domainId() default "";

  boolean isDeprecated() default false;

  int version() default 0;

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createOrganizationId() default "";

  String __typename() default "";

  String $id() default "";

  String $name() default "";

  String $typeName() default "";

  String $ofTypeName() default "";

  String $ofType() default "";

  String $description() default "";

  String $args() default "";

  String $type() default "";

  String $deprecationReason() default "";

  String $from() default "";

  String $to() default "";

  String $withType() default "";

  String $withFrom() default "";

  String $withTo() default "";

  String $domainId() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createOrganizationId() default "";

  String $__typename() default "";

  String $argsAggregate() default "";

  String $argsConnection() default "";

  __TypeInput2 ofType() default @__TypeInput2;

  __InputValueInput2[] args() default {};

  __TypeInput2 type() default @__TypeInput2;

  __InputValueInput2 argsAggregate() default @__InputValueInput2;

  __InputValueConnectionInput2 argsConnection() default @__InputValueConnectionInput2;
}
