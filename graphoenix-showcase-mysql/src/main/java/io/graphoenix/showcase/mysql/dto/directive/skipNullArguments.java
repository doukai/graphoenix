package io.graphoenix.showcase.mysql.dto.directive;

import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface skipNullArguments {
  @Name("if")
  boolean _if();
}
