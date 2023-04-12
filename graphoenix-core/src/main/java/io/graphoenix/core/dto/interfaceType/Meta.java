package io.graphoenix.core.dto.interfaceType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import org.eclipse.microprofile.graphql.Interface;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface Meta {
  Boolean isDeprecated = null;

  Integer version = null;

  String realmId = null;

  String createUserId = null;

  LocalDateTime createTime = null;

  String updateUserId = null;

  LocalDateTime updateTime = null;

  String createGroupId = null;

  Boolean getIsDeprecated();

  void setIsDeprecated(Boolean isDeprecated);

  Integer getVersion();

  void setVersion(Integer version);

  String getRealmId();

  void setRealmId(String realmId);

  String getCreateUserId();

  void setCreateUserId(String createUserId);

  LocalDateTime getCreateTime();

  void setCreateTime(LocalDateTime createTime);

  String getUpdateUserId();

  void setUpdateUserId(String updateUserId);

  LocalDateTime getUpdateTime();

  void setUpdateTime(LocalDateTime updateTime);

  String getCreateGroupId();

  void setCreateGroupId(String createGroupId);
}
