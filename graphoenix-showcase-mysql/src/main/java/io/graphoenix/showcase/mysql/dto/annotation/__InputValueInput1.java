package io.graphoenix.showcase.mysql.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __InputValueInput1 {
  String id() default "";

  String name() default "";

  String typeName() default "";

  String ofTypeName() default "";

  int fieldId() default 0;

  String directiveName() default "";

  String description() default "";

  String defaultValue() default "";

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

  String $fieldId() default "";

  String $directiveName() default "";

  String $description() default "";

  String $type() default "";

  String $defaultValue() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  StringInput2 name() default "";

  StringInput2 typeName() default "";

  StringInput2 ofTypeName() default "";

  __TypeInput2 ofType() default @__TypeInput2;

  StringInput2 directiveName() default "";

  StringInput2 description() default "";

  __TypeInput2 type() default @__TypeInput2;

  StringInput2 defaultValue() default "";

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

  StringInput2 directiveNameMax() default "";

  StringInput2 directiveNameMin() default "";

  StringInput2 descriptionMax() default "";

  StringInput2 descriptionMin() default "";

  StringInput2 defaultValueMax() default "";

  StringInput2 defaultValueMin() default "";

  __InputValueInput2[] list() default {};

  String $list() default "";
}
