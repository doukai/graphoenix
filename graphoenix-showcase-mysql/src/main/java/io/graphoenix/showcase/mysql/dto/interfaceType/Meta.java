package io.graphoenix.showcase.mysql.dto.interfaceType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import org.eclipse.microprofile.graphql.Interface;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public interface Meta {
  String domainId = null;

  Boolean isDeprecated = null;

  Integer version = null;

  String createUserId = null;

  LocalDateTime createTime = null;

  String updateUserId = null;

  LocalDateTime updateTime = null;

  String createOrganizationId = null;

  String getDomainId();

  void setDomainId(String domainId);

  Boolean getIsDeprecated();

  void setIsDeprecated(Boolean isDeprecated);

  Integer getVersion();

  void setVersion(Integer version);

  String getCreateUserId();

  void setCreateUserId(String createUserId);

  LocalDateTime getCreateTime();

  void setCreateTime(LocalDateTime createTime);

  String getUpdateUserId();

  void setUpdateUserId(String updateUserId);

  LocalDateTime getUpdateTime();

  void setUpdateTime(LocalDateTime updateTime);

  String getCreateOrganizationId();

  void setCreateOrganizationId(String createOrganizationId);
}
