package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OrganizationArgumentSet {
  private OrganizationArgument id;

  private OrganizationArgument aboveId;

  private OrganizationArgumentSet above;

  private OrganizationArgument name;

  private OrganizationArgument version;

  private OrganizationArgument isDeprecated;

  private OrganizationArgument __typename;

  public OrganizationArgument getId() {
    return this.id;
  }

  public void setId(OrganizationArgument id) {
    this.id = id;
  }

  public OrganizationArgument getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(OrganizationArgument aboveId) {
    this.aboveId = aboveId;
  }

  public OrganizationArgumentSet getAbove() {
    return this.above;
  }

  public void setAbove(OrganizationArgumentSet above) {
    this.above = above;
  }

  public OrganizationArgument getName() {
    return this.name;
  }

  public void setName(OrganizationArgument name) {
    this.name = name;
  }

  public OrganizationArgument getVersion() {
    return this.version;
  }

  public void setVersion(OrganizationArgument version) {
    this.version = version;
  }

  public OrganizationArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(OrganizationArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public OrganizationArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(OrganizationArgument __typename) {
    this.__typename = __typename;
  }
}
