package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class UserPhonesOrderBy {
  private Sort id;

  private Sort userId;

  private Sort phone;

  private Sort version;

  private Sort isDeprecated;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getUserId() {
    return this.userId;
  }

  public void setUserId(Sort userId) {
    this.userId = userId;
  }

  public Sort getPhone() {
    return this.phone;
  }

  public void setPhone(Sort phone) {
    this.phone = phone;
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
