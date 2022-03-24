package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class RoleArgumentSet {
  private RoleArgument id;

  private RoleArgument name;

  private RoleArgument version;

  private RoleArgument isDeprecated;

  private RoleArgument __typename;

  public RoleArgument getId() {
    return this.id;
  }

  public void setId(RoleArgument id) {
    this.id = id;
  }

  public RoleArgument getName() {
    return this.name;
  }

  public void setName(RoleArgument name) {
    this.name = name;
  }

  public RoleArgument getVersion() {
    return this.version;
  }

  public void setVersion(RoleArgument version) {
    this.version = version;
  }

  public RoleArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(RoleArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public RoleArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(RoleArgument __typename) {
    this.__typename = __typename;
  }
}
