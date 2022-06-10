package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface OrganizationExpression1 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  int[] aboveId() default {};

  String[] name() default {};

  boolean[] isDeprecated() default {};

  int[] version() default {};

  String[] realmId() default {};

  String[] createUserId() default {};

  String[] createTime() default {};

  String[] updateUserId() default {};

  String[] updateTime() default {};

  String[] createGroupId() default {};

  String[] __typename() default {};

  int[] orgLevel2() default {};

  int[] orgLevel3() default {};

  boolean[] roleDisable() default {};

  String[] $id() default {};

  String[] $aboveId() default {};

  String[] $name() default {};

  String[] $isDeprecated() default {};

  String[] $version() default {};

  String[] $realmId() default {};

  String[] $createUserId() default {};

  String[] $createTime() default {};

  String[] $updateUserId() default {};

  String[] $updateTime() default {};

  String[] $createGroupId() default {};

  String[] $__typename() default {};

  String[] $orgLevel2() default {};

  String[] $orgLevel3() default {};

  String[] $roleDisable() default {};

  OrganizationExpressions2[] above() default {};

  UserExpressions2[] users() default {};

  UserExpressions2[] usersAggregate() default {};

  UserConnectionExpressions2[] usersConnection() default {};

  UserExpressions2[] userByOrg() default {};

  OrganizationExpressions2[] parent() default {};
}
