package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
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
public class __Directive implements Meta {
  @NonNull
  private Collection<__InputValue> args;

  private Boolean onFragment;

  @Id
  private String name;

  @NonNull
  private Collection<__DirectiveLocation> locations;

  private Integer version;

  private Integer schemaId;

  private Boolean onOperation;

  private Boolean onField;

  private String description;

  private Boolean isDeprecated;

  public Collection<__InputValue> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValue> args) {
    this.args = args;
  }

  public Boolean getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(Boolean onFragment) {
    this.onFragment = onFragment;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<__DirectiveLocation> getLocations() {
    return this.locations;
  }

  public void setLocations(Collection<__DirectiveLocation> locations) {
    this.locations = locations;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  public Integer getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Integer schemaId) {
    this.schemaId = schemaId;
  }

  public Boolean getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(Boolean onOperation) {
    this.onOperation = onOperation;
  }

  public Boolean getOnField() {
    return this.onField;
  }

  public void setOnField(Boolean onField) {
    this.onField = onField;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }
}
