package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeOrderBy1 {
  Sort name() default Sort.ASC;

  Sort schemaId() default Sort.ASC;

  Sort kind() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

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

  __FieldOrderBy2 fields() default @__FieldOrderBy2;

  __TypeOrderBy2 interfaces() default @__TypeOrderBy2;

  __TypeOrderBy2 possibleTypes() default @__TypeOrderBy2;

  __EnumValueOrderBy2 enumValues() default @__EnumValueOrderBy2;

  __InputValueOrderBy2 inputFields() default @__InputValueOrderBy2;

  __TypeOrderBy2 ofType() default @__TypeOrderBy2;
}
