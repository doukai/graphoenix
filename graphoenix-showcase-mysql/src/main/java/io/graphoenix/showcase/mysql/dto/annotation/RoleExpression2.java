package io.graphoenix.showcase.mysql.dto.annotation;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.TypeExpression;
import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Name("RoleExpression")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@TypeExpression
public @interface RoleExpression2 {
  Operator opr() default Operator.EQ;

  boolean[] isDeprecated() default {};

  String[] name() default {};

  String[] id() default {};

  int[] version() default {};

  String[] $isDeprecated() default {};

  String[] $name() default {};

  String[] $id() default {};

  String[] $version() default {};

  String[] $users() default {};
}
