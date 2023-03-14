package io.graphoenix.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.mysql.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __TypeInput {
  private String name;

  private Integer schemaId;

  private __TypeKind kind;

  private String description;

  private Collection<__FieldInput> fields;

  private Collection<__TypeInput> interfaces;

  private Collection<__TypeInput> possibleTypes;

  private Collection<__EnumValueInput> enumValues;

  private Collection<__InputValueInput> inputFields;

  private String ofTypeName;

  private __TypeInput ofType;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  @DefaultValue("\"__Type\"")
  private String __typename;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Integer schemaId) {
    this.schemaId = schemaId;
  }

  public __TypeKind getKind() {
    return this.kind;
  }

  public void setKind(__TypeKind kind) {
    this.kind = kind;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<__FieldInput> getFields() {
    return this.fields;
  }

  public void setFields(Collection<__FieldInput> fields) {
    this.fields = fields;
  }

  public Collection<__TypeInput> getInterfaces() {
    return this.interfaces;
  }

  public void setInterfaces(Collection<__TypeInput> interfaces) {
    this.interfaces = interfaces;
  }

  public Collection<__TypeInput> getPossibleTypes() {
    return this.possibleTypes;
  }

  public void setPossibleTypes(Collection<__TypeInput> possibleTypes) {
    this.possibleTypes = possibleTypes;
  }

  public Collection<__EnumValueInput> getEnumValues() {
    return this.enumValues;
  }

  public void setEnumValues(Collection<__EnumValueInput> enumValues) {
    this.enumValues = enumValues;
  }

  public Collection<__InputValueInput> getInputFields() {
    return this.inputFields;
  }

  public void setInputFields(Collection<__InputValueInput> inputFields) {
    this.inputFields = inputFields;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __TypeInput getOfType() {
    return this.ofType;
  }

  public void setOfType(__TypeInput ofType) {
    this.ofType = ofType;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getRealmId() {
    return this.realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public String getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public String getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(String updateUserId) {
    this.updateUserId = updateUserId;
  }

  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public String getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(String createGroupId) {
    this.createGroupId = createGroupId;
  }

  public String get__typename() {
    return this.__typename;
  }

  public void set__typename(String __typename) {
    this.__typename = __typename;
  }
}
