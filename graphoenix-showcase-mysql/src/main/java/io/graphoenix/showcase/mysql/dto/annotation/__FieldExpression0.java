package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface __FieldExpression0 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  String[] name() default {};

  String[] typeName() default {};

  String[] ofTypeName() default {};

  String[] description() default {};

  String[] deprecationReason() default {};

  String[] from() default {};

  String[] to() default {};

  String[] withType() default {};

  String[] withFrom() default {};

  String[] withTo() default {};

  boolean[] isDeprecated() default {};

  int[] version() default {};

  String[] realmId() default {};

  String[] createUserId() default {};

  String[] createTime() default {};

  String[] updateUserId() default {};

  String[] updateTime() default {};

  String[] createGroupId() default {};

  String[] __typename() default {};

  String[] $id() default {};

  String[] $name() default {};

  String[] $typeName() default {};

  String[] $ofTypeName() default {};

  String[] $description() default {};

  String[] $deprecationReason() default {};

  String[] $from() default {};

  String[] $to() default {};

  String[] $withType() default {};

  String[] $withFrom() default {};

  String[] $withTo() default {};

  String[] $isDeprecated() default {};

  String[] $version() default {};

  String[] $realmId() default {};

  String[] $createUserId() default {};

  String[] $createTime() default {};

  String[] $updateUserId() default {};

  String[] $updateTime() default {};

  String[] $createGroupId() default {};

  String[] $__typename() default {};

  __TypeExpressions1[] ofType() default {};

  __InputValueExpressions1[] args() default {};

  __TypeExpressions1[] type() default {};

  __InputValueExpressions1[] argsAggregate() default {};

  __InputValueConnectionExpressions1[] argsConnection() default {};
}
