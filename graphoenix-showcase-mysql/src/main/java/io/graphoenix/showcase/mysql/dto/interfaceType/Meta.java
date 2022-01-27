package io.graphoenix.showcase.mysql.dto.interfaceType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Interface;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public interface Meta {
  Boolean isDeprecated = null;

  Integer version = null;

  Boolean getIsDeprecated();

  void setIsDeprecated(Boolean isDeprecated);

  Integer getVersion();

  void setVersion(Integer version);
}
