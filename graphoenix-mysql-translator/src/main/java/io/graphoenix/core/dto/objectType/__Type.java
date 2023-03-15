package io.graphoenix.core.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.__TypeKind;
import io.graphoenix.mysql.dto.objectType.__EnumValue;
import io.graphoenix.mysql.dto.objectType.__Field;
import io.graphoenix.mysql.dto.objectType.__InputValue;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __Type {
  @Id
  @NonNull
  private String name;

  private Integer schemaId;

  @NonNull
  private __TypeKind kind;

  private String description;

  private Collection<__Field> fields;

  private Collection<io.graphoenix.mysql.dto.objectType.__Type> interfaces;

  private Collection<io.graphoenix.mysql.dto.objectType.__Type> possibleTypes;

  private Collection<__EnumValue> enumValues;

  private Collection<__InputValue> inputFields;

  private String ofTypeName;

  private io.graphoenix.mysql.dto.objectType.__Type ofType;

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

  public Collection<io.graphoenix.mysql.dto.objectType.__Type> getInterfaces() {
    return this.interfaces;
  }

  public void setInterfaces(Collection<io.graphoenix.mysql.dto.objectType.__Type> interfaces) {
    this.interfaces = interfaces;
  }

  public Collection<io.graphoenix.mysql.dto.objectType.__Type> getPossibleTypes() {
    return this.possibleTypes;
  }

  public void setPossibleTypes(
      Collection<io.graphoenix.mysql.dto.objectType.__Type> possibleTypes) {
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

  public io.graphoenix.mysql.dto.objectType.__Type getOfType() {
    return this.ofType;
  }

  public void setOfType(io.graphoenix.mysql.dto.objectType.__Type ofType) {
    this.ofType = ofType;
  }
}
