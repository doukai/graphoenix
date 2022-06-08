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
public @interface UserPhonesExpression0 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  int[] userId() default {};

  String[] phone() default {};

  String[] domainId() default {};

  boolean[] isDeprecated() default {};

  int[] version() default {};

  String[] createUserId() default {};

  String[] createTime() default {};

  String[] updateUserId() default {};

  String[] updateTime() default {};

  String[] createOrganizationId() default {};

  String[] __typename() default {};

  String[] $id() default {};

  String[] $userId() default {};

  String[] $phone() default {};

  String[] $domainId() default {};

  String[] $isDeprecated() default {};

  String[] $version() default {};

  String[] $createUserId() default {};

  String[] $createTime() default {};

  String[] $updateUserId() default {};

  String[] $updateTime() default {};

  String[] $createOrganizationId() default {};

  String[] $__typename() default {};
}
