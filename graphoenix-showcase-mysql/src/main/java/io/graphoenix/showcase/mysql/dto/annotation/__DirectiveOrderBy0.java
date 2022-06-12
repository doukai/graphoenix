package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __DirectiveOrderBy0 {
  Sort name() default Sort.ASC;

  Sort schemaId() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort locations() default Sort.ASC;

  Sort onOperation() default Sort.ASC;

  Sort onFragment() default Sort.ASC;

  Sort onField() default Sort.ASC;

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

  String $description() default "";

  String $locations() default "";

  String $args() default "";

  String $onOperation() default "";

  String $onFragment() default "";

  String $onField() default "";

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

  __InputValueOrderBy1 args() default @__InputValueOrderBy1;
}
