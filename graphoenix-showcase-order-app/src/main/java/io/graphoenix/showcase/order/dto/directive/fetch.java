package io.graphoenix.showcase.order.dto.directive;

import io.graphoenix.showcase.order.dto.enumType.Protocol;
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
@Target({ElementType.FIELD})
public @interface fetch {
  Protocol protocol();

  String from();

  MapWith with();

  String to();

  boolean anchor();
}
