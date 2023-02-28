package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __FieldExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression name() default @StringExpression;

  StringExpression typeName() default @StringExpression;

  StringExpression ofTypeName() default @StringExpression;

  StringExpression description() default @StringExpression;

  StringExpression deprecationReason() default @StringExpression;

  StringExpression from() default @StringExpression;

  StringExpression to() default @StringExpression;

  StringExpression withType() default @StringExpression;

  StringExpression withFrom() default @StringExpression;

  StringExpression withTo() default @StringExpression;

  BooleanExpression isDeprecated() default @BooleanExpression;

  IntExpression version() default @IntExpression;

  StringExpression realmId() default @StringExpression;

  StringExpression createUserId() default @StringExpression;

  StringExpression createTime() default @StringExpression;

  StringExpression updateUserId() default @StringExpression;

  StringExpression updateTime() default @StringExpression;

  StringExpression createGroupId() default @StringExpression;

  StringExpression __typename() default @StringExpression;

  String[] groupBy() default {};

  __FieldOrderBy0 orderBy() default @__FieldOrderBy0;

  int first() default 0;

  String $first() default "";

  int last() default 0;

  String $last() default "";

  int offset() default 0;

  String $offset() default "";

  String after() default "";

  String before() default "";

  String $after() default "";

  String $before() default "";

  StringExpression1 name() default @StringExpression1;

  StringExpression1 typeName() default @StringExpression1;

  StringExpression1 ofTypeName() default @StringExpression1;

  __TypeExpression1 ofType() default @__TypeExpression1;

  StringExpression1 description() default @StringExpression1;

  __InputValueExpression1 args() default @__InputValueExpression1;

  __TypeExpression1 type() default @__TypeExpression1;

  StringExpression1 deprecationReason() default @StringExpression1;

  StringExpression1 from() default @StringExpression1;

  StringExpression1 to() default @StringExpression1;

  StringExpression1 withType() default @StringExpression1;

  StringExpression1 withFrom() default @StringExpression1;

  StringExpression1 withTo() default @StringExpression1;

  StringExpression1 realmId() default @StringExpression1;

  StringExpression1 createUserId() default @StringExpression1;

  StringExpression1 updateUserId() default @StringExpression1;

  StringExpression1 createGroupId() default @StringExpression1;

  StringExpression1 __typename() default @StringExpression1;

  StringExpression1 nameMax() default @StringExpression1;

  StringExpression1 nameMin() default @StringExpression1;

  StringExpression1 typeNameMax() default @StringExpression1;

  StringExpression1 typeNameMin() default @StringExpression1;

  StringExpression1 ofTypeNameMax() default @StringExpression1;

  StringExpression1 ofTypeNameMin() default @StringExpression1;

  StringExpression1 descriptionMax() default @StringExpression1;

  StringExpression1 descriptionMin() default @StringExpression1;

  StringExpression1 deprecationReasonMax() default @StringExpression1;

  StringExpression1 deprecationReasonMin() default @StringExpression1;

  StringExpression1 fromMax() default @StringExpression1;

  StringExpression1 fromMin() default @StringExpression1;

  StringExpression1 toMax() default @StringExpression1;

  StringExpression1 toMin() default @StringExpression1;

  StringExpression1 withTypeMax() default @StringExpression1;

  StringExpression1 withTypeMin() default @StringExpression1;

  StringExpression1 withFromMax() default @StringExpression1;

  StringExpression1 withFromMin() default @StringExpression1;

  StringExpression1 withToMax() default @StringExpression1;

  StringExpression1 withToMin() default @StringExpression1;

  __FieldExpression1[] exs() default {};
}
