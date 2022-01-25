package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __FieldInput {
  @NonNull
  private String id;

  private String name;

  private String typeName;

  private String ofTypeName;

  private String description;

  @NonNull
  private Collection<__InputValueInput> args;

  @NonNull
  private __TypeInput type;

  private String deprecationReason;

  private Integer version;

  private Boolean isDeprecated;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<__InputValueInput> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValueInput> args) {
    this.args = args;
  }

  public __TypeInput getType() {
    return this.type;
  }

  public void setType(__TypeInput type) {
    this.type = type;
  }

  public String getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(String deprecationReason) {
    this.deprecationReason = deprecationReason;
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
}
