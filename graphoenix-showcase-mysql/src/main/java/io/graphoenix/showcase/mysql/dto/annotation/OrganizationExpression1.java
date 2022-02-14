package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Name("OrganizationExpression")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface OrganizationExpression1 {
  Operator opr() default Operator.EQ;

  int[] aboveId() default {};

  boolean[] isDeprecated() default {};

  String[] name() default {};

  int[] orgLevel2() default {};

  int[] orgLevel3() default {};

  boolean[] roleDisable() default {};

  String[] id() default {};

  int[] version() default {};

  String[] $parent() default {};

  String[] $aboveId() default {};

  String[] $isDeprecated() default {};

  String[] $userByOrg() default {};

  String[] $above() default {};

  String[] $name() default {};

  String[] $orgLevel2() default {};

  String[] $orgLevel3() default {};

  String[] $roleDisable() default {};

  String[] $id() default {};

  String[] $version() default {};

  String[] $users() default {};

  OrganizationExpressions2[] parent() default {};

  UserExpressions2[] userByOrg() default {};

  OrganizationExpressions2[] above() default {};

  UserExpressions2[] users() default {};
}
