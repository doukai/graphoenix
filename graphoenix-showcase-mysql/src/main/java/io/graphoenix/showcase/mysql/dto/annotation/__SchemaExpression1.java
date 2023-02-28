package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface __SchemaExpression1 {
  Conditional cond() default Conditional.AND;

  IDExpression id() default @IDExpression;

  StringExpression queryTypeName() default @StringExpression;

  StringExpression mutationTypeName() default @StringExpression;

  StringExpression subscriptionTypeName() default @StringExpression;

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

  __SchemaOrderBy1 orderBy() default @__SchemaOrderBy1;

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

  StringExpression2 queryTypeName() default @StringExpression2;

  StringExpression2 mutationTypeName() default @StringExpression2;

  StringExpression2 subscriptionTypeName() default @StringExpression2;

  __TypeExpression2 types() default @__TypeExpression2;

  __TypeExpression2 queryType() default @__TypeExpression2;

  __TypeExpression2 mutationType() default @__TypeExpression2;

  __TypeExpression2 subscriptionType() default @__TypeExpression2;

  __DirectiveExpression2 directives() default @__DirectiveExpression2;

  StringExpression2 realmId() default @StringExpression2;

  StringExpression2 createUserId() default @StringExpression2;

  StringExpression2 updateUserId() default @StringExpression2;

  StringExpression2 createGroupId() default @StringExpression2;

  StringExpression2 __typename() default @StringExpression2;

  StringExpression2 queryTypeNameMax() default @StringExpression2;

  StringExpression2 queryTypeNameMin() default @StringExpression2;

  StringExpression2 mutationTypeNameMax() default @StringExpression2;

  StringExpression2 mutationTypeNameMin() default @StringExpression2;

  StringExpression2 subscriptionTypeNameMax() default @StringExpression2;

  StringExpression2 subscriptionTypeNameMin() default @StringExpression2;

  __SchemaExpression2[] exs() default {};
}
