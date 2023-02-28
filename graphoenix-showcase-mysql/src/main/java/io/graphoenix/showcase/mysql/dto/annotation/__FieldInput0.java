package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldInput0 {
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

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

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

  StringInput1 name() default "";

  StringInput1 typeName() default "";

  StringInput1 ofTypeName() default "";

  __TypeInput1 ofType() default @__TypeInput1;

  StringInput1 description() default "";

  __InputValueInput1[] args() default {};

  __TypeInput1 type() default @__TypeInput1;

  StringInput1 deprecationReason() default "";

  StringInput1 from() default "";

  StringInput1 to() default "";

  StringInput1 withType() default "";

  StringInput1 withFrom() default "";

  StringInput1 withTo() default "";

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  StringInput1 nameMax() default "";

  StringInput1 nameMin() default "";

  StringInput1 typeNameMax() default "";

  StringInput1 typeNameMin() default "";

  StringInput1 ofTypeNameMax() default "";

  StringInput1 ofTypeNameMin() default "";

  StringInput1 descriptionMax() default "";

  StringInput1 descriptionMin() default "";

  StringInput1 deprecationReasonMax() default "";

  StringInput1 deprecationReasonMin() default "";

  StringInput1 fromMax() default "";

  StringInput1 fromMin() default "";

  StringInput1 toMax() default "";

  StringInput1 toMin() default "";

  StringInput1 withTypeMax() default "";

  StringInput1 withTypeMin() default "";

  StringInput1 withFromMax() default "";

  StringInput1 withFromMin() default "";

  StringInput1 withToMax() default "";

  StringInput1 withToMin() default "";

  __FieldInput1[] list() default {};

  String $list() default "";
}
