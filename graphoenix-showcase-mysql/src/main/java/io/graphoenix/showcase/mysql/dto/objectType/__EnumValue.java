package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __EnumValue implements Meta {
  private Integer version;

  private String deprecationReason;

  @Id
  private String id;

  private String name;

  private Boolean isDeprecated;

  private String ofTypeName;

  private String description;

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(String deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

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

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
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
}
