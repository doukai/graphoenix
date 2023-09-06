package io.graphoenix.core.dto.inputObjectType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface MetaInput {
  Integer getVersion();

  void setVersion(Integer version);

  Integer getRealmId();

  void setRealmId(Integer realmId);

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
