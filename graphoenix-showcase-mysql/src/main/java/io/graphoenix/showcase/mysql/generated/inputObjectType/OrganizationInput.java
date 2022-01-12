package io.graphoenix.showcase.mysql.generated.inputObjectType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Set;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OrganizationInput {
  private Integer id;

  private Integer aboveId;

  private OrganizationInput above;

  private Set<UserInput> users;

  @NonNull
  private String name;

  private Integer version;

  private Boolean isDeprecated;

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
