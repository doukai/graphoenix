package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __EnumValueInput0 {
  String id() default "";

  String name() default "";

  String ofTypeName() default "";

  String description() default "";

  String deprecationReason() default "";

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

  String $ofTypeName() default "";

  String $ofType() default "";

  String $description() default "";

  String $deprecationReason() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  StringInput1 name() default "";

  StringInput1 ofTypeName() default "";

  __TypeInput1 ofType() default @__TypeInput1;

  StringInput1 description() default "";

  StringInput1 deprecationReason() default "";

  StringInput1 realmId() default "";

  StringInput1 createUserId() default "";

  StringInput1 updateUserId() default "";

  StringInput1 createGroupId() default "";

  StringInput1 __typename() default "";

  StringInput1 nameMax() default "";

  StringInput1 nameMin() default "";

  StringInput1 ofTypeNameMax() default "";

  StringInput1 ofTypeNameMin() default "";

  StringInput1 descriptionMax() default "";

  StringInput1 descriptionMin() default "";

  StringInput1 deprecationReasonMax() default "";

  StringInput1 deprecationReasonMin() default "";

  __EnumValueInput1[] list() default {};

  String $list() default "";
}
