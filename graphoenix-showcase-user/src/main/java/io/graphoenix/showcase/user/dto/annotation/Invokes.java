package io.graphoenix.showcase.user.dto.annotation;

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
@Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
public @interface Invokes {
  Invoke[] list();
}
