package io.graphoenix.showcase.mysql.dto.interfaceType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Interface;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public interface Meta {
  Integer version = null;

  Boolean isDeprecated = null;

  Integer getVersion();

  void setVersion(Integer version);

  Boolean getIsDeprecated();

  void setIsDeprecated(Boolean isDeprecated);
}
