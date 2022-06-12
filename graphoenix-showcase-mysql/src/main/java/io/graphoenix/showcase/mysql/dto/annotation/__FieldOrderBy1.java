package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldOrderBy1 {
  Sort id() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort typeName() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort deprecationReason() default Sort.ASC;

  Sort from() default Sort.ASC;

  Sort to() default Sort.ASC;

  Sort withType() default Sort.ASC;

  Sort withFrom() default Sort.ASC;

  Sort withTo() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

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

  __TypeOrderBy2 ofType() default @__TypeOrderBy2;

  __InputValueOrderBy2 args() default @__InputValueOrderBy2;

  __TypeOrderBy2 type() default @__TypeOrderBy2;
}
