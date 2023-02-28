package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
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

  StringInput2 name() default "";

  StringInput2 typeName() default "";

  StringInput2 ofTypeName() default "";

  __TypeInput2 ofType() default @__TypeInput2;

  StringInput2 description() default "";

  __InputValueInput2[] args() default {};

  __TypeInput2 type() default @__TypeInput2;

  StringInput2 deprecationReason() default "";

  StringInput2 from() default "";

  StringInput2 to() default "";

  StringInput2 withType() default "";

  StringInput2 withFrom() default "";

  StringInput2 withTo() default "";

  StringInput2 realmId() default "";

  StringInput2 createUserId() default "";

  StringInput2 updateUserId() default "";

  StringInput2 createGroupId() default "";

  StringInput2 __typename() default "";

  StringInput2 nameMax() default "";

  StringInput2 nameMin() default "";

  StringInput2 typeNameMax() default "";

  StringInput2 typeNameMin() default "";

  StringInput2 ofTypeNameMax() default "";

  StringInput2 ofTypeNameMin() default "";

  StringInput2 descriptionMax() default "";

  StringInput2 descriptionMin() default "";

  StringInput2 deprecationReasonMax() default "";

  StringInput2 deprecationReasonMin() default "";

  StringInput2 fromMax() default "";

  StringInput2 fromMin() default "";

  StringInput2 toMax() default "";

  StringInput2 toMin() default "";

  StringInput2 withTypeMax() default "";

  StringInput2 withTypeMin() default "";

  StringInput2 withFromMax() default "";

  StringInput2 withFromMin() default "";

  StringInput2 withToMax() default "";

  StringInput2 withToMin() default "";

  __FieldInput2[] list() default {};

  String $list() default "";
}
