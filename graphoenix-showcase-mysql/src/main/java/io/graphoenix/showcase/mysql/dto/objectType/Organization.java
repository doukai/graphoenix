package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class Organization implements Meta {
  @Id
  private String id;

  @NonNull
  private String name;

  private Integer orgLevel2;

  private Organization above;

  private Boolean isDeprecated;

  private Integer version;

  private Collection<User> userByOrg;

  private Integer aboveId;

  private Collection<Integer> orgLevel3;

  private Collection<User> users;

  private Boolean roleDisable;

  private Organization parent;

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

  public Integer getOrgLevel2() {
    return this.orgLevel2;
  }

  public void setOrgLevel2(Integer orgLevel2) {
    this.orgLevel2 = orgLevel2;
  }

  public Organization getAbove() {
    return this.above;
  }

  public void setAbove(Organization above) {
    this.above = above;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  public Collection<User> getUserByOrg() {
    return this.userByOrg;
  }

  public void setUserByOrg(Collection<User> userByOrg) {
    this.userByOrg = userByOrg;
  }

  public Integer getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(Integer aboveId) {
    this.aboveId = aboveId;
  }

  public Collection<Integer> getOrgLevel3() {
    return this.orgLevel3;
  }

  public void setOrgLevel3(Collection<Integer> orgLevel3) {
    this.orgLevel3 = orgLevel3;
  }

  public Collection<User> getUsers() {
    return this.users;
  }

  public void setUsers(Collection<User> users) {
    this.users = users;
  }

  public Boolean getRoleDisable() {
    return this.roleDisable;
  }

  public void setRoleDisable(Boolean roleDisable) {
    this.roleDisable = roleDisable;
  }

  public Organization getParent() {
    return this.parent;
  }

  public void setParent(Organization parent) {
    this.parent = parent;
  }
}
