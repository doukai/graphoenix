package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserArgumentSet {
  private UserArgument id;

  private UserArgument login;

  private UserArgument password;

  private UserArgument name;

  private UserArgument age;

  private UserArgument disable;

  private UserArgument sex;

  private UserArgument organizationId;

  private OrganizationArgumentSet organization;

  private UserArgument version;

  private UserArgument isDeprecated;

  private UserArgument __typename;

  public UserArgument getId() {
    return this.id;
  }

  public void setId(UserArgument id) {
    this.id = id;
  }

  public UserArgument getLogin() {
    return this.login;
  }

  public void setLogin(UserArgument login) {
    this.login = login;
  }

  public UserArgument getPassword() {
    return this.password;
  }

  public void setPassword(UserArgument password) {
    this.password = password;
  }

  public UserArgument getName() {
    return this.name;
  }

  public void setName(UserArgument name) {
    this.name = name;
  }

  public UserArgument getAge() {
    return this.age;
  }

  public void setAge(UserArgument age) {
    this.age = age;
  }

  public UserArgument getDisable() {
    return this.disable;
  }

  public void setDisable(UserArgument disable) {
    this.disable = disable;
  }

  public UserArgument getSex() {
    return this.sex;
  }

  public void setSex(UserArgument sex) {
    this.sex = sex;
  }

  public UserArgument getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(UserArgument organizationId) {
    this.organizationId = organizationId;
  }

  public OrganizationArgumentSet getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationArgumentSet organization) {
    this.organization = organization;
  }

  public UserArgument getVersion() {
    return this.version;
  }

  public void setVersion(UserArgument version) {
    this.version = version;
  }

  public UserArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(UserArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public UserArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(UserArgument __typename) {
    this.__typename = __typename;
  }
}
