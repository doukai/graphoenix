package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class RoleInput {
  private String id;

  @NonNull
  private String name;

  private Collection<UserInput> users;

  private Integer version;

  private Boolean isDeprecated;

  @DefaultValue("\"Role\"")
  @NonNull
  private String __typename;

  private UserInput usersAggregate;

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

  public Collection<UserInput> getUsers() {
    return this.users;
  }

  public void setUsers(Collection<UserInput> users) {
    this.users = users;
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

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
    this.__typename = __typename;
  }

  public UserInput getUsersAggregate() {
    return this.usersAggregate;
  }

  public void setUsersAggregate(UserInput usersAggregate) {
    this.usersAggregate = usersAggregate;
  }
}
