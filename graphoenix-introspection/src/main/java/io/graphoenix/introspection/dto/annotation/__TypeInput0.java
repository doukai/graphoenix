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

  __TypeKind kind() default __TypeKind.SCALAR;

  String description() default "";

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  int schemaId() default 0;

  String ofTypeName() default "";

  String $name() default "";

  String $ofSchema() default "";

  String $kind() default "";

  String $description() default "";

  String $fields() default "";

  String $interfaces() default "";

  String $possibleTypes() default "";

  String $enumValues() default "";

  String $inputFields() default "";

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

  String $schemaId() default "";

  String $ofTypeName() default "";

  String $__typeInterfaces() default "";

  String $__typeInterfacesAggregate() default "";

  String $__typeInterfacesConnection() default "";

  String $__typePossibleTypes() default "";

  String $__typePossibleTypesAggregate() default "";

  String $__typePossibleTypesConnection() default "";
}
