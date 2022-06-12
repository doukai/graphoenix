package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserOrderBy1 {
  Sort id() default Sort.ASC;

  Sort login() default Sort.ASC;

  Sort password() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort age() default Sort.ASC;

  Sort disable() default Sort.ASC;

  Sort sex() default Sort.ASC;

  Sort organizationId() default Sort.ASC;

  Sort phones() default Sort.ASC;

  Sort test1() default Sort.ASC;

  Sort test2() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  Sort userDetail2() default Sort.ASC;

  String $id() default "";

  String $login() default "";

  String $password() default "";

  String $name() default "";

  String $age() default "";

  String $disable() default "";

  String $sex() default "";

  String $organizationId() default "";

  String $organization() default "";

  String $roles() default "";

  String $phones() default "";

  String $test1() default "";

  String $test2() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $rolesAggregate() default "";

  String $rolesConnection() default "";

  String $userDetail2() default "";

  OrganizationOrderBy2 organization() default @OrganizationOrderBy2;

  RoleOrderBy2 roles() default @RoleOrderBy2;
}
