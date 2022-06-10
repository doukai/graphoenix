package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.TypeInput;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeInput
public @interface __TypeInput0 {
  String name() default "";

  int schemaId() default 0;

  __TypeKind kind() default __TypeKind.SCALAR;

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

  String $interfacesAggregate() default "";

  String $possibleTypesAggregate() default "";

  String $enumValuesAggregate() default "";

  String $inputFieldsAggregate() default "";

  String $fieldsConnection() default "";

  String $interfacesConnection() default "";

  String $possibleTypesConnection() default "";

  String $enumValuesConnection() default "";

  String $inputFieldsConnection() default "";

  __FieldInput1[] fields() default {};

  __TypeInput1[] interfaces() default {};

  __TypeInput1[] possibleTypes() default {};

  __EnumValueInput1[] enumValues() default {};

  __InputValueInput1[] inputFields() default {};

  __TypeInput1 ofType() default @__TypeInput1;

  __FieldInput1 fieldsAggregate() default @__FieldInput1;

  __TypeInput1 interfacesAggregate() default @__TypeInput1;

  __TypeInput1 possibleTypesAggregate() default @__TypeInput1;

  __EnumValueInput1 enumValuesAggregate() default @__EnumValueInput1;

  __InputValueInput1 inputFieldsAggregate() default @__InputValueInput1;

  __FieldConnectionInput1 fieldsConnection() default @__FieldConnectionInput1;

  __TypeConnectionInput1 interfacesConnection() default @__TypeConnectionInput1;

  __TypeConnectionInput1 possibleTypesConnection() default @__TypeConnectionInput1;

  __EnumValueConnectionInput1 enumValuesConnection() default @__EnumValueConnectionInput1;

  __InputValueConnectionInput1 inputFieldsConnection() default @__InputValueConnectionInput1;
}
