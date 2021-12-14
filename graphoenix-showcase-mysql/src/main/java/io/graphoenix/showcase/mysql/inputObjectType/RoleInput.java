package io.graphoenix.showcase.mysql.inputObjectType;

import java.lang.Integer;
import java.lang.String;
import java.util.Set;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
public class RoleInput {
  private Integer id;

  @NonNull
  private String name;

  private Set<UserInput> users;

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
}
