package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.showcase.mysql.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface UserPhonesExpression {
  Operator opr() default Operator.EQ;

  String[] phone() default {};

  int[] id() default {};

  int[] userId() default {};

  String[] $phone() default {};

  String[] $id() default {};

  String[] $userId() default {};
}
