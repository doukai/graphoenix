package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserRoleArgumentSet {
  private UserRoleArgument id;

  private UserRoleArgument userId;

  private UserRoleArgument roleId;

  private UserRoleArgument version;

  private UserRoleArgument isDeprecated;

  private UserRoleArgument __typename;

  public UserRoleArgument getId() {
    return this.id;
  }

  public void setId(UserRoleArgument id) {
    this.id = id;
  }

  public UserRoleArgument getUserId() {
    return this.userId;
  }

  public void setUserId(UserRoleArgument userId) {
    this.userId = userId;
  }

  public UserRoleArgument getRoleId() {
    return this.roleId;
  }

  public void setRoleId(UserRoleArgument roleId) {
    this.roleId = roleId;
  }

  public UserRoleArgument getVersion() {
    return this.version;
  }

  public void setVersion(UserRoleArgument version) {
    this.version = version;
  }

  public UserRoleArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(UserRoleArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public UserRoleArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(UserRoleArgument __typename) {
    this.__typename = __typename;
  }
}
