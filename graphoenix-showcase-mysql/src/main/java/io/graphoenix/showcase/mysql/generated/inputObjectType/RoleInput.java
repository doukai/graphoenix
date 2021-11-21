package io.graphoenix.showcase.mysql.generated.inputObjectType;

import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class RoleInput {
  private Integer id;

  @NotNull
  private String name;

  private Set<UserInput> users;

  private Integer version;

  private Boolean isDeprecated;

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<UserInput> getUsers() {
    return this.users;
  }

  public void setUsers(Set<UserInput> users) {
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
}
