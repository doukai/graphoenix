package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface __TypeExpression0 {
  Operator opr() default Operator.EQ;

  String[] name() default {};

  int[] schemaId() default {};

  __TypeKind[] kind() default {};

  String[] description() default {};

  String[] ofTypeName() default {};

  boolean[] isDeprecated() default {};

  int[] version() default {};

  String[] realmId() default {};

  String[] createUserId() default {};

  String[] createTime() default {};

  String[] updateUserId() default {};

  String[] updateTime() default {};

  String[] createGroupId() default {};

  String[] __typename() default {};

  String[] $name() default {};

  String[] $schemaId() default {};

  String[] $kind() default {};

  String[] $description() default {};

  String[] $ofTypeName() default {};

  String[] $isDeprecated() default {};

  String[] $version() default {};

  String[] $realmId() default {};

  String[] $createUserId() default {};

  String[] $createTime() default {};

  String[] $updateUserId() default {};

  String[] $updateTime() default {};

  String[] $createGroupId() default {};

  String[] $__typename() default {};

  __FieldExpressions1[] fields() default {};

  __TypeExpressions1[] interfaces() default {};

  __TypeExpressions1[] possibleTypes() default {};

  __EnumValueExpressions1[] enumValues() default {};

  __InputValueExpressions1[] inputFields() default {};

  __TypeExpressions1[] ofType() default {};

  __FieldExpressions1[] fieldsAggregate() default {};

  __TypeExpressions1[] interfacesAggregate() default {};

  __TypeExpressions1[] possibleTypesAggregate() default {};

  __EnumValueExpressions1[] enumValuesAggregate() default {};

  __InputValueExpressions1[] inputFieldsAggregate() default {};

  __FieldConnectionExpressions1[] fieldsConnection() default {};

  __TypeConnectionExpressions1[] interfacesConnection() default {};

  __TypeConnectionExpressions1[] possibleTypesConnection() default {};

  __EnumValueConnectionExpressions1[] enumValuesConnection() default {};

  __InputValueConnectionExpressions1[] inputFieldsConnection() default {};
}
