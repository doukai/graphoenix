package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldOrderBy1 {
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

  StringOrderBy2 name() default @StringOrderBy2;

  StringOrderBy2 typeName() default @StringOrderBy2;

  StringOrderBy2 ofTypeName() default @StringOrderBy2;

  __TypeOrderBy2 ofType() default @__TypeOrderBy2;

  StringOrderBy2 description() default @StringOrderBy2;

  __InputValueOrderBy2 args() default @__InputValueOrderBy2;

  __TypeOrderBy2 type() default @__TypeOrderBy2;

  StringOrderBy2 deprecationReason() default @StringOrderBy2;

  StringOrderBy2 from() default @StringOrderBy2;

  StringOrderBy2 to() default @StringOrderBy2;

  StringOrderBy2 withType() default @StringOrderBy2;

  StringOrderBy2 withFrom() default @StringOrderBy2;

  StringOrderBy2 withTo() default @StringOrderBy2;

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

  StringOrderBy2 descriptionMax() default @StringOrderBy2;

  StringOrderBy2 descriptionMin() default @StringOrderBy2;

  StringOrderBy2 deprecationReasonMax() default @StringOrderBy2;

  StringOrderBy2 deprecationReasonMin() default @StringOrderBy2;

  StringOrderBy2 fromMax() default @StringOrderBy2;

  StringOrderBy2 fromMin() default @StringOrderBy2;

  StringOrderBy2 toMax() default @StringOrderBy2;

  StringOrderBy2 toMin() default @StringOrderBy2;

  StringOrderBy2 withTypeMax() default @StringOrderBy2;

  StringOrderBy2 withTypeMin() default @StringOrderBy2;

  StringOrderBy2 withFromMax() default @StringOrderBy2;

  StringOrderBy2 withFromMin() default @StringOrderBy2;

  StringOrderBy2 withToMax() default @StringOrderBy2;

  StringOrderBy2 withToMin() default @StringOrderBy2;
}
