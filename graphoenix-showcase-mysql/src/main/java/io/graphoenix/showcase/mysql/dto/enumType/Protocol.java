package io.graphoenix.showcase.mysql.dto.enumType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Enum;

@Enum
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public enum Protocol {
  LOCAL,

  GRPC,

  HTTP,

  RSOCKET
}
