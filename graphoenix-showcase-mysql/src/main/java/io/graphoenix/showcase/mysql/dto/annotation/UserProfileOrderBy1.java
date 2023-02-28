package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserProfileOrderBy1 {
  Sort id() default Sort.ASC;

  Sort userId() default Sort.ASC;

  Sort email() default Sort.ASC;

  Sort address() default Sort.ASC;

  Sort qq() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  StringOrderBy2 userId() default @StringOrderBy2;

  StringOrderBy2 email() default @StringOrderBy2;

  StringOrderBy2 address() default @StringOrderBy2;

  StringOrderBy2 qq() default @StringOrderBy2;

  UserOrderBy2 user() default @UserOrderBy2;

  UserOrderBy2 rpcUser() default @UserOrderBy2;

  StringOrderBy2 realmId() default @StringOrderBy2;

  StringOrderBy2 createUserId() default @StringOrderBy2;

  StringOrderBy2 updateUserId() default @StringOrderBy2;

  StringOrderBy2 createGroupId() default @StringOrderBy2;

  StringOrderBy2 __typename() default @StringOrderBy2;

  StringOrderBy2 userIdMax() default @StringOrderBy2;

  StringOrderBy2 userIdMin() default @StringOrderBy2;

  StringOrderBy2 emailMax() default @StringOrderBy2;

  StringOrderBy2 emailMin() default @StringOrderBy2;

  StringOrderBy2 addressMax() default @StringOrderBy2;

  StringOrderBy2 addressMin() default @StringOrderBy2;

  StringOrderBy2 qqMax() default @StringOrderBy2;

  StringOrderBy2 qqMin() default @StringOrderBy2;
}
