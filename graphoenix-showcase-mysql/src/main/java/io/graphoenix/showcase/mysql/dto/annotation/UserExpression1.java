package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface UserExpression1 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  String[] login() default {};

  String[] password() default {};

  String[] name() default {};

  int[] age() default {};

  boolean[] disable() default {};

  Sex[] sex() default {};

  int[] organizationId() default {};

  String[] phones() default {};

  int[] test1() default {};

  boolean[] test2() default {};

  boolean[] isDeprecated() default {};

  int[] version() default {};

  String[] realmId() default {};

  String[] createUserId() default {};

  String[] createTime() default {};

  String[] updateUserId() default {};

  String[] updateTime() default {};

  String[] createGroupId() default {};

  String[] __typename() default {};

  String[] userDetail2() default {};

  String[] $id() default {};

  String[] $login() default {};

  String[] $password() default {};

  String[] $name() default {};

  String[] $age() default {};

  String[] $disable() default {};

  String[] $sex() default {};

  String[] $organizationId() default {};

  String[] $phones() default {};

  String[] $test1() default {};

  String[] $test2() default {};

  String[] $isDeprecated() default {};

  String[] $version() default {};

  String[] $realmId() default {};

  String[] $createUserId() default {};

  String[] $createTime() default {};

  String[] $updateUserId() default {};

  String[] $updateTime() default {};

  String[] $createGroupId() default {};

  String[] $__typename() default {};

  String[] $userDetail2() default {};

  OrganizationExpressions2[] organization() default {};

  RoleExpressions2[] roles() default {};

  RoleExpressions2[] rolesAggregate() default {};

  RoleConnectionExpressions2[] rolesConnection() default {};
}
