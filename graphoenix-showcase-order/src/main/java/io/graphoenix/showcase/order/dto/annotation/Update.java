package io.graphoenix.showcase.order.dto.annotation;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface Update {
  @Name("if")
  boolean _if() default false;
}
