package io.graphoenix.showcase.order.dto.annotation;

import io.graphoenix.core.dto.annotation.Invoke;
import io.graphoenix.spi.annotation.Directive;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
@Documented
@Retention(RetentionPolicy.SOURCE)
@Directive("invokes")
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
public @interface Invokes {
  Invoke[] list();
}
