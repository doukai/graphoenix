package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveInput0 {
  String name() default "";

  int schemaId() default 0;

  String description() default "";

  __DirectiveLocation[] locations() default {};

  boolean onOperation() default false;

  boolean onFragment() default false;

  boolean onField() default false;

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  String $name() default "";

  String $schemaId() default "";

  String $description() default "";

  String $locations() default "";

  String $args() default "";

  String $onOperation() default "";

  String $onFragment() default "";

  String $onField() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $argsAggregate() default "";

  String $argsConnection() default "";

  StringInput1 description() default "";

  __InputValueInput1[] args() default {};

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  StringInput1 descriptionMax() default "";

  StringInput1 descriptionMin() default "";

  __DirectiveInput1[] list() default {};

  String $list() default "";
}
