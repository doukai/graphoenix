package io.graphoenix.showcase.order.dto.annotation;

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
@Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
public @interface PackageInfo {
  String packageName();

  String grpcPackageName();
}
