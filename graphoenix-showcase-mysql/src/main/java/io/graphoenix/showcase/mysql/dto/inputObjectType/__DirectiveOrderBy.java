package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __DirectiveOrderBy {
  private Sort name;

  private Sort schemaId;

  private Sort description;

  private Sort onOperation;

  private Sort onFragment;

  private Sort onField;

  private Sort domainId;

  private Sort isDeprecated;

  private Sort version;

  private Sort createUserId;

  private Sort createTime;

  private Sort updateUserId;

  private Sort updateTime;

  private Sort createOrganizationId;

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

  public Sort getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(Sort onOperation) {
    this.onOperation = onOperation;
  }

  public Sort getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(Sort onFragment) {
    this.onFragment = onFragment;
  }

  public Sort getOnField() {
    return this.onField;
  }

  public void setOnField(Sort onField) {
    this.onField = onField;
  }

  public Sort getDomainId() {
    return this.domainId;
  }

  public void setDomainId(Sort domainId) {
    this.domainId = domainId;
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

  public Sort getCreateOrganizationId() {
    return this.createOrganizationId;
  }

  public void setCreateOrganizationId(Sort createOrganizationId) {
    this.createOrganizationId = createOrganizationId;
  }

  public Sort get__Typename() {
    return this.__typename;
  }

  public void set__Typename(Sort __typename) {
    this.__typename = __typename;
  }
}
