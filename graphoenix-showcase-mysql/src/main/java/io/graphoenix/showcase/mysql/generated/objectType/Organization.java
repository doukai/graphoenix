package io.graphoenix.showcase.mysql.generated.objectType;

import io.graphoenix.showcase.mysql.generated.interfaceType.Meta;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

import java.util.Set;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class Organization implements Meta {
  @Id
  private Integer id;

  private Integer aboveId;

  private Organization above;

  private Set<User> users;

  @NonNull
  private String name;

  private Integer version;

  private Boolean isDeprecated;

  private String roleMark;

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

  public Organization getAbove() {
    return this.above;
  }

  public void setAbove(Organization above) {
    this.above = above;
  }

  public Set<User> getUsers() {
    return this.users;
  }

  public void setUsers(Set<User> users) {
    this.users = users;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String getRoleMark() {
    return roleMark;
  }

  public void setRoleMark(String roleMark) {
    this.roleMark = roleMark;
  }
}
