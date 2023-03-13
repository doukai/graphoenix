package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationOrderBy0 {
  Sort id() default Sort.ASC;

  Sort aboveId() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort orgLevel3() default Sort.ASC;

  Sort roleDisable() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  OrganizationOrderBy1 above() default @OrganizationOrderBy1;

  UserOrderBy1 users() default @UserOrderBy1;

  UserOrderBy1 rpcUsers() default @UserOrderBy1;

  UserOrderBy1 userByOrg() default @UserOrderBy1;

  OrganizationOrderBy1 parent() default @OrganizationOrderBy1;
}
