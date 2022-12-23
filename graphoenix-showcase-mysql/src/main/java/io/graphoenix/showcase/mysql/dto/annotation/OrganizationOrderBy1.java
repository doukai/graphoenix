package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OrganizationOrderBy1 {
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

  Sort orgLevel3() default Sort.ASC;

  Sort roleDisable() default Sort.ASC;

  OrganizationOrderBy2 above() default @OrganizationOrderBy2;

  UserOrderBy2 users() default @UserOrderBy2;

  UserOrderBy2 rpcUsers() default @UserOrderBy2;

  UserOrderBy2 userByOrg() default @UserOrderBy2;

  OrganizationOrderBy2 parent() default @OrganizationOrderBy2;
}
