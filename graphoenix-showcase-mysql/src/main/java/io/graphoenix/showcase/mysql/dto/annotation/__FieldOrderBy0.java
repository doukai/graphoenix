package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldOrderBy0 {
  Sort id() default Sort.ASC;

  Sort name() default Sort.ASC;

  Sort typeName() default Sort.ASC;

  Sort ofTypeName() default Sort.ASC;

  Sort description() default Sort.ASC;

  Sort deprecationReason() default Sort.ASC;

  Sort from() default Sort.ASC;

  Sort to() default Sort.ASC;

  Sort withType() default Sort.ASC;

  Sort withFrom() default Sort.ASC;

  Sort withTo() default Sort.ASC;

  Sort isDeprecated() default Sort.ASC;

  Sort version() default Sort.ASC;

  Sort realmId() default Sort.ASC;

  Sort createUserId() default Sort.ASC;

  Sort createTime() default Sort.ASC;

  Sort updateUserId() default Sort.ASC;

  Sort updateTime() default Sort.ASC;

  Sort createGroupId() default Sort.ASC;

  Sort __typename() default Sort.ASC;

  StringOrderBy1 name() default @StringOrderBy1;

  StringOrderBy1 typeName() default @StringOrderBy1;

  StringOrderBy1 ofTypeName() default @StringOrderBy1;

  __TypeOrderBy1 ofType() default @__TypeOrderBy1;

  StringOrderBy1 description() default @StringOrderBy1;

  __InputValueOrderBy1 args() default @__InputValueOrderBy1;

  __TypeOrderBy1 type() default @__TypeOrderBy1;

  StringOrderBy1 deprecationReason() default @StringOrderBy1;

  StringOrderBy1 from() default @StringOrderBy1;

  StringOrderBy1 to() default @StringOrderBy1;

  StringOrderBy1 withType() default @StringOrderBy1;

  StringOrderBy1 withFrom() default @StringOrderBy1;

  StringOrderBy1 withTo() default @StringOrderBy1;

  StringOrderBy1 realmId() default @StringOrderBy1;

  StringOrderBy1 createUserId() default @StringOrderBy1;

  StringOrderBy1 updateUserId() default @StringOrderBy1;

  StringOrderBy1 createGroupId() default @StringOrderBy1;

  StringOrderBy1 __typename() default @StringOrderBy1;

  StringOrderBy1 nameMax() default @StringOrderBy1;

  StringOrderBy1 nameMin() default @StringOrderBy1;

  StringOrderBy1 typeNameMax() default @StringOrderBy1;

  StringOrderBy1 typeNameMin() default @StringOrderBy1;

  StringOrderBy1 ofTypeNameMax() default @StringOrderBy1;

  StringOrderBy1 ofTypeNameMin() default @StringOrderBy1;

  StringOrderBy1 descriptionMax() default @StringOrderBy1;

  StringOrderBy1 descriptionMin() default @StringOrderBy1;

  StringOrderBy1 deprecationReasonMax() default @StringOrderBy1;

  StringOrderBy1 deprecationReasonMin() default @StringOrderBy1;

  StringOrderBy1 fromMax() default @StringOrderBy1;

  StringOrderBy1 fromMin() default @StringOrderBy1;

  StringOrderBy1 toMax() default @StringOrderBy1;

  StringOrderBy1 toMin() default @StringOrderBy1;

  StringOrderBy1 withTypeMax() default @StringOrderBy1;

  StringOrderBy1 withTypeMin() default @StringOrderBy1;

  StringOrderBy1 withFromMax() default @StringOrderBy1;

  StringOrderBy1 withFromMin() default @StringOrderBy1;

  StringOrderBy1 withToMax() default @StringOrderBy1;

  StringOrderBy1 withToMin() default @StringOrderBy1;
}
