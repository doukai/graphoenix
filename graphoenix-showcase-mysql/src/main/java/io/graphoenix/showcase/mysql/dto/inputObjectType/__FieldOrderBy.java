package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import null.dto.enumType.Sort;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __FieldOrderBy {
  private Sort id;

  private Sort name;

  private Sort typeName;

  private Sort ofTypeName;

  private Sort description;

  private Sort deprecationReason;

  private Sort from;

  private Sort to;

  private Sort withType;

  private Sort withFrom;

  private Sort withTo;

  private Sort isDeprecated;

  private Sort version;

  private Sort realmId;

  private Sort createUserId;

  private Sort createTime;

  private Sort updateUserId;

  private Sort updateTime;

  private Sort createGroupId;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getName() {
    return this.name;
  }

  public void setName(Sort name) {
    this.name = name;
  }

  public Sort getTypeName() {
    return this.typeName;
  }

  public void setTypeName(Sort typeName) {
    this.typeName = typeName;
  }

  public Sort getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(Sort ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public Sort getDescription() {
    return this.description;
  }

  public void setDescription(Sort description) {
    this.description = description;
  }

  public Sort getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(Sort deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public Sort getFrom() {
    return this.from;
  }

  public void setFrom(Sort from) {
    this.from = from;
  }

  public Sort getTo() {
    return this.to;
  }

  public void setTo(Sort to) {
    this.to = to;
  }

  public Sort getWithType() {
    return this.withType;
  }

  public void setWithType(Sort withType) {
    this.withType = withType;
  }

  public Sort getWithFrom() {
    return this.withFrom;
  }

  public void setWithFrom(Sort withFrom) {
    this.withFrom = withFrom;
  }

  public Sort getWithTo() {
    return this.withTo;
  }

  public void setWithTo(Sort withTo) {
    this.withTo = withTo;
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

  public Sort get__typename() {
    return this.__typename;
  }

  public void set__typename(Sort __typename) {
    this.__typename = __typename;
  }
}
