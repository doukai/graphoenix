package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class __TypeOrderBy {
  private Sort name;

  private Sort schemaId;

  private Sort description;

  private Sort ofTypeName;

  private Sort isDeprecated;

  private Sort version;

  private Sort realmId;

  private Sort createUserId;

  private Sort createTime;

  private Sort updateUserId;

  private Sort updateTime;

  private Sort createGroupId;

  private Sort __typename;

  public Sort getName() {
    return this.name;
  }

  public void setName(Sort name) {
    this.name = name;
  }

  public Sort getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Sort schemaId) {
    this.schemaId = schemaId;
  }

  public Sort getDescription() {
    return this.description;
  }

  public void setDescription(Sort description) {
    this.description = description;
  }

  public Sort getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(Sort ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public Sort getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Sort isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Sort getVersion() {
    return this.version;
  }

  public void setVersion(Sort version) {
    this.version = version;
  }

  public Sort getRealmId() {
    return this.realmId;
  }

  public void setRealmId(Sort realmId) {
    this.realmId = realmId;
  }

  public Sort getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(Sort createUserId) {
    this.createUserId = createUserId;
  }

  public Sort getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(Sort createTime) {
    this.createTime = createTime;
  }

  public Sort getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(Sort updateUserId) {
    this.updateUserId = updateUserId;
  }

  public Sort getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(Sort updateTime) {
    this.updateTime = updateTime;
  }

  public Sort getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(Sort createGroupId) {
    this.createGroupId = createGroupId;
  }

  public Sort get__Typename() {
    return this.__typename;
  }

  public void set__Typename(Sort __typename) {
    this.__typename = __typename;
  }
}
