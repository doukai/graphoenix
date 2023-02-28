package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeOrderBy0 {
  Sort name() default Sort.ASC;

  Sort schemaId() default Sort.ASC;

  Sort kind() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  StringOrderBy1 description() default @StringOrderBy1;

  __FieldOrderBy1 fields() default @__FieldOrderBy1;

  __TypeOrderBy1 interfaces() default @__TypeOrderBy1;

  __TypeOrderBy1 possibleTypes() default @__TypeOrderBy1;

  __EnumValueOrderBy1 enumValues() default @__EnumValueOrderBy1;

  __InputValueOrderBy1 inputFields() default @__InputValueOrderBy1;

  StringOrderBy1 ofTypeName() default @StringOrderBy1;

  __TypeOrderBy1 ofType() default @__TypeOrderBy1;

  StringOrderBy1 realmId() default @StringOrderBy1;

  StringOrderBy1 createUserId() default @StringOrderBy1;

  StringOrderBy1 updateUserId() default @StringOrderBy1;

  StringOrderBy1 createGroupId() default @StringOrderBy1;

  StringOrderBy1 __typename() default @StringOrderBy1;

  StringOrderBy1 descriptionMax() default @StringOrderBy1;

  StringOrderBy1 descriptionMin() default @StringOrderBy1;

  StringOrderBy1 ofTypeNameMax() default @StringOrderBy1;

  StringOrderBy1 ofTypeNameMin() default @StringOrderBy1;
}
