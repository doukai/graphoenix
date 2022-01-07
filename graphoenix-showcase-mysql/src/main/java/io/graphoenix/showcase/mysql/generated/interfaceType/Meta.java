package io.graphoenix.showcase.mysql.generated.interfaceType;

import java.lang.Boolean;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Interface;

@Interface
public interface Meta {
  Integer version = null;

  Boolean isDeprecated = null;

  Integer getVersion();

  void setVersion(Integer version);

  Boolean getIsDeprecated();

  void setIsDeprecated(Boolean isDeprecated);
}