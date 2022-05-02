package io.graphoenix.showcase.mysql.dto.enumType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Enum;

@Enum
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public enum RoleType {
  ADMIN,

  USER,

  ANONYMOUS
}
