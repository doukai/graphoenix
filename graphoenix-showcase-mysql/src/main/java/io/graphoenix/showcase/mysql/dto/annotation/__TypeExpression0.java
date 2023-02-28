package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeExpression0 {
  Conditional cond() default Conditional.AND;

  IDExpression name() default @IDExpression;

  IntExpression schemaId() default @IntExpression;

  __TypeKindExpression kind() default @__TypeKindExpression;

  StringExpression description() default @StringExpression;

  StringExpression ofTypeName() default @StringExpression;

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

  __TypeOrderBy0 orderBy() default @__TypeOrderBy0;

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

  StringExpression1 description() default @StringExpression1;

  __FieldExpression1 fields() default @__FieldExpression1;

  __TypeExpression1 interfaces() default @__TypeExpression1;

  __TypeExpression1 possibleTypes() default @__TypeExpression1;

  __EnumValueExpression1 enumValues() default @__EnumValueExpression1;

  __InputValueExpression1 inputFields() default @__InputValueExpression1;

  StringExpression1 ofTypeName() default @StringExpression1;

  __TypeExpression1 ofType() default @__TypeExpression1;

  StringExpression1 realmId() default @StringExpression1;

  StringExpression1 createUserId() default @StringExpression1;

  StringExpression1 updateUserId() default @StringExpression1;

  StringExpression1 createGroupId() default @StringExpression1;

  StringExpression1 __typename() default @StringExpression1;

  StringExpression1 descriptionMax() default @StringExpression1;

  StringExpression1 descriptionMin() default @StringExpression1;

  StringExpression1 ofTypeNameMax() default @StringExpression1;

  StringExpression1 ofTypeNameMin() default @StringExpression1;

  __TypeExpression1[] exs() default {};
}
