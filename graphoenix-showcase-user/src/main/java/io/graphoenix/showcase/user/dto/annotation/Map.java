package io.graphoenix.showcase.user.dto.annotation;

import io.graphoenix.core.dto.annotation.MapWith;
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
@Directive("map")
@Target({ElementType.FIELD})
public @interface Map {
  String from();

  MapWith with();

  String to();

  boolean anchor();
}
