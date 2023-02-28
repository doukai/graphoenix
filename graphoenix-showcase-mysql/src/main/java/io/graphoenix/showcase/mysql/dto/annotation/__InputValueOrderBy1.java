package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __InputValueOrderBy1 {
  Sort id() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort typeName() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort fieldId() default Sort.ASC;

  Sort directiveName() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort defaultValue() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  StringOrderBy2 name() default @StringOrderBy2;

  StringOrderBy2 typeName() default @StringOrderBy2;

  StringOrderBy2 ofTypeName() default @StringOrderBy2;

  __TypeOrderBy2 ofType() default @__TypeOrderBy2;

  StringOrderBy2 directiveName() default @StringOrderBy2;

  StringOrderBy2 description() default @StringOrderBy2;

  __TypeOrderBy2 type() default @__TypeOrderBy2;

  StringOrderBy2 defaultValue() default @StringOrderBy2;

  StringOrderBy2 realmId() default @StringOrderBy2;

  StringOrderBy2 createUserId() default @StringOrderBy2;

  StringOrderBy2 updateUserId() default @StringOrderBy2;

  StringOrderBy2 createGroupId() default @StringOrderBy2;

  StringOrderBy2 __typename() default @StringOrderBy2;

  StringOrderBy2 nameMax() default @StringOrderBy2;

  StringOrderBy2 nameMin() default @StringOrderBy2;

  StringOrderBy2 typeNameMax() default @StringOrderBy2;

  StringOrderBy2 typeNameMin() default @StringOrderBy2;

  StringOrderBy2 ofTypeNameMax() default @StringOrderBy2;

  StringOrderBy2 ofTypeNameMin() default @StringOrderBy2;

  StringOrderBy2 directiveNameMax() default @StringOrderBy2;

  StringOrderBy2 directiveNameMin() default @StringOrderBy2;

  StringOrderBy2 descriptionMax() default @StringOrderBy2;

  StringOrderBy2 descriptionMin() default @StringOrderBy2;

  StringOrderBy2 defaultValueMax() default @StringOrderBy2;

  StringOrderBy2 defaultValueMin() default @StringOrderBy2;
}
