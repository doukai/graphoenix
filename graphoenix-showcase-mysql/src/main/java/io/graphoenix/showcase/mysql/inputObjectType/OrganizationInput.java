package io.graphoenix.showcase.mysql.inputObjectType;

import java.lang.Integer;
import java.lang.String;
import java.util.Set;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
public class OrganizationInput {
  private Integer id;

  private Integer aboveId;

  private OrganizationInput above;

  private Set<UserInput> users;

  @NonNull
  private String name;

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(Integer aboveId) {
    this.aboveId = aboveId;
  }

  public OrganizationInput getAbove() {
    return this.above;
  }

  public void setAbove(OrganizationInput above) {
    this.above = above;
  }

  public Set<UserInput> getUsers() {
    return this.users;
  }

  public void setUsers(Set<UserInput> users) {
    this.users = users;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
