package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class __TypeInterfaces implements Meta {
  @Id
  private String id;

  @NonNull
  private String typeName;

  @NonNull
  private String interfaceName;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer typeNameCount;

  private String typeNameMax;

  private String typeNameMin;

  private Integer interfaceNameCount;

  private String interfaceNameMax;

  private String interfaceNameMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getInterfaceName() {
    return this.interfaceName;
  }

  public void setInterfaceName(String interfaceName) {
    this.interfaceName = interfaceName;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String getRealmId() {
    return this.realmId;
  }

  @Override
  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  @Override
  public String getCreateUserId() {
    return this.createUserId;
  }

  @Override
  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  @Override
  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  @Override
  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  @Override
  public String getUpdateUserId() {
    return this.updateUserId;
  }

  @Override
  public void setUpdateUserId(String updateUserId) {
    this.updateUserId = updateUserId;
  }

  @Override
  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  @Override
  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public String getCreateGroupId() {
    return this.createGroupId;
  }

  @Override
  public void setCreateGroupId(String createGroupId) {
    this.createGroupId = createGroupId;
  }

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
    this.__typename = __typename;
  }

  public Integer getIdCount() {
    return this.idCount;
  }

  public void setIdCount(Integer idCount) {
    this.idCount = idCount;
  }

  public String getIdMax() {
    return this.idMax;
  }

  public void setIdMax(String idMax) {
    this.idMax = idMax;
  }

  public String getIdMin() {
    return this.idMin;
  }

  public void setIdMin(String idMin) {
    this.idMin = idMin;
  }

  public Integer getTypeNameCount() {
    return this.typeNameCount;
  }

  public void setTypeNameCount(Integer typeNameCount) {
    this.typeNameCount = typeNameCount;
  }

  public String getTypeNameMax() {
    return this.typeNameMax;
  }

  public void setTypeNameMax(String typeNameMax) {
    this.typeNameMax = typeNameMax;
  }

  public String getTypeNameMin() {
    return this.typeNameMin;
  }

  public void setTypeNameMin(String typeNameMin) {
    this.typeNameMin = typeNameMin;
  }

  public Integer getInterfaceNameCount() {
    return this.interfaceNameCount;
  }

  public void setInterfaceNameCount(Integer interfaceNameCount) {
    this.interfaceNameCount = interfaceNameCount;
  }

  public String getInterfaceNameMax() {
    return this.interfaceNameMax;
  }

  public void setInterfaceNameMax(String interfaceNameMax) {
    this.interfaceNameMax = interfaceNameMax;
  }

  public String getInterfaceNameMin() {
    return this.interfaceNameMin;
  }

  public void setInterfaceNameMin(String interfaceNameMin) {
    this.interfaceNameMin = interfaceNameMin;
  }
}
