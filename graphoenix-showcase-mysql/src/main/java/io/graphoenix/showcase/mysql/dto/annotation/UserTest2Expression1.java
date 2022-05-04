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
public @interface UserTest2Expression1 {
  Operator opr() default Operator.EQ;

  String[] id() default {};

  int[] userId() default {};

  boolean[] test2() default {};

  int[] version() default {};

  boolean[] isDeprecated() default {};

  String[] __typename() default {};

  String[] $id() default {};

  String[] $userId() default {};

  String[] $test2() default {};

  String[] $version() default {};

  String[] $isDeprecated() default {};

  String[] $__typename() default {};
}