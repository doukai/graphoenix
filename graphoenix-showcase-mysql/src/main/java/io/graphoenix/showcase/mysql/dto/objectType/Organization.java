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
  private Collection<User> userByOrg;

  private Boolean roleDisable;

  @Id
  private String id;

  private Integer version;

  @NonNull
  private String name;

  private Integer orgLevel2;

  private Integer aboveId;

  private Boolean isDeprecated;

  private Collection<Integer> orgLevel3;

  private Organization above;

  private Organization parent;

  private Collection<User> users;

  public Collection<User> getUserByOrg() {
    return this.userByOrg;
  }

  public void setUserByOrg(Collection<User> userByOrg) {
    this.userByOrg = userByOrg;
  }

  public Boolean getRoleDisable() {
    return this.roleDisable;
  }

  public void setRoleDisable(Boolean roleDisable) {
    this.roleDisable = roleDisable;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
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

  public Integer getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(Integer aboveId) {
    this.aboveId = aboveId;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Collection<Integer> getOrgLevel3() {
    return this.orgLevel3;
  }

  public void setOrgLevel3(Collection<Integer> orgLevel3) {
    this.orgLevel3 = orgLevel3;
  }

  public Organization getAbove() {
    return this.above;
  }

  public void setAbove(Organization above) {
    this.above = above;
  }

  public Organization getParent() {
    return this.parent;
  }

  public void setParent(Organization parent) {
    this.parent = parent;
  }

  public Collection<User> getUsers() {
    return this.users;
  }

  public void setUsers(Collection<User> users) {
    this.users = users;
  }
}
