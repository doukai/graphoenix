package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
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

  StringOrderBy2 login() default @StringOrderBy2;

  StringOrderBy2 password() default @StringOrderBy2;

  StringOrderBy2 name() default @StringOrderBy2;

  UserProfileOrderBy2 userProfile() default @UserProfileOrderBy2;

  UserProfileOrderBy2 rpcUserProfile() default @UserProfileOrderBy2;

  OrganizationOrderBy2 rpcOrganization() default @OrganizationOrderBy2;

  OrganizationOrderBy2 organization() default @OrganizationOrderBy2;

  RoleOrderBy2 roles() default @RoleOrderBy2;

  StringOrderBy2 phones() default @StringOrderBy2;

  StringOrderBy2 realmId() default @StringOrderBy2;

  StringOrderBy2 createUserId() default @StringOrderBy2;

  StringOrderBy2 updateUserId() default @StringOrderBy2;

  StringOrderBy2 createGroupId() default @StringOrderBy2;

  StringOrderBy2 __typename() default @StringOrderBy2;

  StringOrderBy2 loginMax() default @StringOrderBy2;

  StringOrderBy2 loginMin() default @StringOrderBy2;

  StringOrderBy2 passwordMax() default @StringOrderBy2;

  StringOrderBy2 passwordMin() default @StringOrderBy2;

  StringOrderBy2 nameMax() default @StringOrderBy2;

  StringOrderBy2 nameMin() default @StringOrderBy2;

  StringOrderBy2 userDetail2() default @StringOrderBy2;
}
