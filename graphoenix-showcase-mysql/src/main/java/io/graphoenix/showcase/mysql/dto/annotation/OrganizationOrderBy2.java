package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationOrderBy2 {
  Sort id() default Sort.ASC;

  Sort aboveId() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  Sort orgLevel2() default Sort.ASC;

  Sort orgLevel3() default Sort.ASC;

  Sort roleDisable() default Sort.ASC;

  String $id() default "";

  String $aboveId() default "";

  String $above() default "";

  String $users() default "";

  String $name() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $usersAggregate() default "";

  String $usersConnection() default "";

  String $orgLevel2() default "";

  String $orgLevel3() default "";

  String $roleDisable() default "";

  String $userByOrg() default "";

  String $parent() default "";
}
