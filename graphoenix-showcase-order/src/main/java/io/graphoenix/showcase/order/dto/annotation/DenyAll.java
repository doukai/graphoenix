package io.graphoenix.showcase.order.dto.annotation;

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
@Directive("denyAll")
@Target({ElementType.FIELD,ElementType.TYPE})
public @interface DenyAll {
}
