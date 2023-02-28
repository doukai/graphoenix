package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserOrderBy0 {
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

  StringOrderBy1 login() default @StringOrderBy1;

  StringOrderBy1 password() default @StringOrderBy1;

  StringOrderBy1 name() default @StringOrderBy1;

  UserProfileOrderBy1 userProfile() default @UserProfileOrderBy1;

  UserProfileOrderBy1 rpcUserProfile() default @UserProfileOrderBy1;

  OrganizationOrderBy1 rpcOrganization() default @OrganizationOrderBy1;

  OrganizationOrderBy1 organization() default @OrganizationOrderBy1;

  RoleOrderBy1 roles() default @RoleOrderBy1;

  StringOrderBy1 phones() default @StringOrderBy1;

  StringOrderBy1 realmId() default @StringOrderBy1;

  StringOrderBy1 createUserId() default @StringOrderBy1;

  StringOrderBy1 updateUserId() default @StringOrderBy1;

  StringOrderBy1 createGroupId() default @StringOrderBy1;

  StringOrderBy1 __typename() default @StringOrderBy1;

  StringOrderBy1 loginMax() default @StringOrderBy1;

  StringOrderBy1 loginMin() default @StringOrderBy1;

  StringOrderBy1 passwordMax() default @StringOrderBy1;

  StringOrderBy1 passwordMin() default @StringOrderBy1;

  StringOrderBy1 nameMax() default @StringOrderBy1;

  StringOrderBy1 nameMin() default @StringOrderBy1;

  StringOrderBy1 userDetail2() default @StringOrderBy1;
}
