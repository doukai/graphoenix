package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.__TypeKind;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeInput {
  @NonNull
  private String name;

  private Integer schemaId;

  @NonNull
  private __TypeKind kind;

  private String description;

  private Collection<__FieldInput> fields;

  private Collection<__TypeInput> interfaces;

  private Collection<__TypeInput> possibleTypes;

  private Collection<__EnumValueInput> enumValues;

  private Collection<__InputValueInput> inputFields;

  private String ofTypeName;

  private __TypeInput ofType;

  private Integer version;

  private Boolean isDeprecated;

  @DefaultValue("\"__Type\"")
  @NonNull
  private String __typename;

  private __FieldInput fieldsAggregate;

  private __TypeInput interfacesAggregate;

  private __TypeInput possibleTypesAggregate;

  private __EnumValueInput enumValuesAggregate;

  private __InputValueInput inputFieldsAggregate;

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

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
    this.__typename = __typename;
  }

  public __FieldInput getFieldsAggregate() {
    return this.fieldsAggregate;
  }

  public void setFieldsAggregate(__FieldInput fieldsAggregate) {
    this.fieldsAggregate = fieldsAggregate;
  }

  public __TypeInput getInterfacesAggregate() {
    return this.interfacesAggregate;
  }

  public void setInterfacesAggregate(__TypeInput interfacesAggregate) {
    this.interfacesAggregate = interfacesAggregate;
  }

  public __TypeInput getPossibleTypesAggregate() {
    return this.possibleTypesAggregate;
  }

  public void setPossibleTypesAggregate(__TypeInput possibleTypesAggregate) {
    this.possibleTypesAggregate = possibleTypesAggregate;
  }

  public __EnumValueInput getEnumValuesAggregate() {
    return this.enumValuesAggregate;
  }

  public void setEnumValuesAggregate(__EnumValueInput enumValuesAggregate) {
    this.enumValuesAggregate = enumValuesAggregate;
  }

  public __InputValueInput getInputFieldsAggregate() {
    return this.inputFieldsAggregate;
  }

  public void setInputFieldsAggregate(__InputValueInput inputFieldsAggregate) {
    this.inputFieldsAggregate = inputFieldsAggregate;
  }
}
