package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __Type implements Meta {
  @Id
  @NonNull
  private String name;

  private Integer schemaId;

  @NonNull
  private __TypeKind kind;

  private String description;

  private Collection<__Field> fields;

  private Collection<__Type> interfaces;

  private Collection<__Type> possibleTypes;

  private Collection<__EnumValue> enumValues;

  private Collection<__InputValue> inputFields;

  private String ofTypeName;

  private __Type ofType;

  private Integer version;

  private Boolean isDeprecated;

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

  public Collection<__Field> getFields() {
    return this.fields;
  }

  public void setFields(Collection<__Field> fields) {
    this.fields = fields;
  }

  public Collection<__Type> getInterfaces() {
    return this.interfaces;
  }

  public void setInterfaces(Collection<__Type> interfaces) {
    this.interfaces = interfaces;
  }

  public Collection<__Type> getPossibleTypes() {
    return this.possibleTypes;
  }

  public void setPossibleTypes(Collection<__Type> possibleTypes) {
    this.possibleTypes = possibleTypes;
  }

  public Collection<__EnumValue> getEnumValues() {
    return this.enumValues;
  }

  public void setEnumValues(Collection<__EnumValue> enumValues) {
    this.enumValues = enumValues;
  }

  public Collection<__InputValue> getInputFields() {
    return this.inputFields;
  }

  public void setInputFields(Collection<__InputValue> inputFields) {
    this.inputFields = inputFields;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __Type getOfType() {
    return this.ofType;
  }

  public void setOfType(__Type ofType) {
    this.ofType = ofType;
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
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
    this.__typename = __typename;
  }
}
