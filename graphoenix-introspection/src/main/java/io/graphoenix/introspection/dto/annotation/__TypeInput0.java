package io.graphoenix.introspection.dto.annotation;

import io.graphoenix.core.dto.enumType.__TypeKind;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeInput0 {
  String name() default "";

  int schemaId() default 0;

  __TypeKind kind() default io.graphoenix.introspection.dto.enumType.__TypeKind.SCALAR;

  String description() default "";

  String ofTypeName() default "";

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

  String $kind() default "";

  String $description() default "";

  String $fields() default "";

  String $interfaces() default "";

  String $possibleTypes() default "";

  String $enumValues() default "";

  String $inputFields() default "";

  String $ofTypeName() default "";

  String $ofType() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $fieldsAggregate() default "";

  String $fieldsConnection() default "";

  String $interfacesAggregate() default "";

  String $interfacesConnection() default "";

  String $possibleTypesAggregate() default "";

  String $possibleTypesConnection() default "";

  String $enumValuesAggregate() default "";

  String $enumValuesConnection() default "";

  String $inputFieldsAggregate() default "";

  String $inputFieldsConnection() default "";
}
