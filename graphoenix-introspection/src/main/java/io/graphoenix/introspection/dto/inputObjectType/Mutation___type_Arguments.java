package io.graphoenix.introspection.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Mutation___type_Arguments {
  private String name;

  private __SchemaInput ofSchema;

  private __TypeKind kind;

  private String description;

  private Collection<__FieldInput> fields;

  private Collection<__TypeInput> interfaces;

  private Collection<__TypeInput> possibleTypes;

  private Collection<__EnumValueInput> enumValues;

  private Collection<__InputValueInput> inputFields;

  private __TypeInput ofType;

  private Boolean isDeprecated;

  private Integer version;

  private Integer realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  @DefaultValue("\"__Type\"")
  private String __typename;

  private Integer schemaId;

  private String ofTypeName;

  private Collection<__TypeInterfacesInput> __typeInterfaces;

  private Collection<__TypePossibleTypesInput> __typePossibleTypes;

  private __TypeExpression where;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public __SchemaInput getOfSchema() {
    return this.ofSchema;
  }

  public void setOfSchema(__SchemaInput ofSchema) {
    this.ofSchema = ofSchema;
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

  public Integer getRealmId() {
    return this.realmId;
  }

  public void setRealmId(Integer realmId) {
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

  public Integer getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Integer schemaId) {
    this.schemaId = schemaId;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public Collection<__TypeInterfacesInput> get__typeInterfaces() {
    return this.__typeInterfaces;
  }

  public void set__typeInterfaces(Collection<__TypeInterfacesInput> __typeInterfaces) {
    this.__typeInterfaces = __typeInterfaces;
  }

  public Collection<__TypePossibleTypesInput> get__typePossibleTypes() {
    return this.__typePossibleTypes;
  }

  public void set__typePossibleTypes(Collection<__TypePossibleTypesInput> __typePossibleTypes) {
    this.__typePossibleTypes = __typePossibleTypes;
  }

  public __TypeExpression getWhere() {
    return this.where;
  }

  public void setWhere(__TypeExpression where) {
    this.where = where;
  }
}
