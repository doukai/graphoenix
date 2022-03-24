package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserPhonesArgumentSet {
  private UserPhonesArgument id;

  private UserPhonesArgument userId;

  private UserPhonesArgument phone;

  private UserPhonesArgument version;

  private UserPhonesArgument isDeprecated;

  private UserPhonesArgument __typename;

  public UserPhonesArgument getId() {
    return this.id;
  }

  public void setId(UserPhonesArgument id) {
    this.id = id;
  }

  public UserPhonesArgument getUserId() {
    return this.userId;
  }

  public void setUserId(UserPhonesArgument userId) {
    this.userId = userId;
  }

  public UserPhonesArgument getPhone() {
    return this.phone;
  }

  public void setPhone(UserPhonesArgument phone) {
    this.phone = phone;
  }

  public UserPhonesArgument getVersion() {
    return this.version;
  }

  public void setVersion(UserPhonesArgument version) {
    this.version = version;
  }

  public UserPhonesArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(UserPhonesArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public UserPhonesArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(UserPhonesArgument __typename) {
    this.__typename = __typename;
  }
}
