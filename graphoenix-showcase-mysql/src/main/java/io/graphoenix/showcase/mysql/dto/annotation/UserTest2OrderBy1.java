package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UserTest2OrderBy1 {
  Sort id() default Sort.ASC;

  Sort userId() default Sort.ASC;

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

  StringOrderBy2 realmId() default @StringOrderBy2;

  StringOrderBy2 createUserId() default @StringOrderBy2;

  StringOrderBy2 updateUserId() default @StringOrderBy2;

  StringOrderBy2 createGroupId() default @StringOrderBy2;

  StringOrderBy2 __typename() default @StringOrderBy2;
}
