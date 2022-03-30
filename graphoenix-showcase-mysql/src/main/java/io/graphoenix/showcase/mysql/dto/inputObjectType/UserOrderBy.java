package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserOrderBy {
  private Sort id;

  private Sort login;

  private Sort password;

  private Sort name;

  private Sort age;

  private Sort disable;

  private Sort organizationId;

  private Sort version;

  private Sort isDeprecated;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getLogin() {
    return this.login;
  }

  public void setLogin(Sort login) {
    this.login = login;
  }

  public Sort getPassword() {
    return this.password;
  }

  public void setPassword(Sort password) {
    this.password = password;
  }

  public Sort getName() {
    return this.name;
  }

  public void setName(Sort name) {
    this.name = name;
  }

  public Sort getAge() {
    return this.age;
  }

  public void setAge(Sort age) {
    this.age = age;
  }

  public Sort getDisable() {
    return this.disable;
  }

  public void setDisable(Sort disable) {
    this.disable = disable;
  }

  public Sort getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(Sort organizationId) {
    this.organizationId = organizationId;
  }

  public Sort getVersion() {
    return this.version;
  }

  public void setVersion(Sort version) {
    this.version = version;
  }

  public Sort getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Sort isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Sort get__Typename() {
    return this.__typename;
  }

  public void set__Typename(Sort __typename) {
    this.__typename = __typename;
  }
}