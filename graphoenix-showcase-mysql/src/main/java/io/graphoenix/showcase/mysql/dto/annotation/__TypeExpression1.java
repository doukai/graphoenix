package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __TypeExpression1 {
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

  __TypeOrderBy1 orderBy() default @__TypeOrderBy1;

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

  __FieldExpression2 fields() default @__FieldExpression2;

  __TypeExpression2 interfaces() default @__TypeExpression2;

  __TypeExpression2 possibleTypes() default @__TypeExpression2;

  __EnumValueExpression2 enumValues() default @__EnumValueExpression2;

  __InputValueExpression2 inputFields() default @__InputValueExpression2;

  __TypeExpression2 ofType() default @__TypeExpression2;

  __TypeExpression2[] exs() default {};
}
