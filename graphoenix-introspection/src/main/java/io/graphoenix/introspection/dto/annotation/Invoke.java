package io.graphoenix.introspection.dto.annotation;

import io.graphoenix.core.dto.annotation.InvokeParameter;
import io.graphoenix.spi.annotation.Directive;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
@Documented
@Retention(RetentionPolicy.SOURCE)
@Directive("invoke")
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
public @interface Invoke {
  String className();

  String methodName();

  InvokeParameter[] parameters();

  String returnClassName();
}
