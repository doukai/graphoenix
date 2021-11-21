package io.graphoenix.showcase.mysql.generated.objectType;

import io.graphoenix.showcase.mysql.generated.interfaceType.Meta;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.util.Set;

public class QueryType implements Meta {
  private Role role;

  private Set<Role> roleList;

  private User user;

  private Set<User> userList;

  private Organization organization;

  private Set<Organization> organizationList;

  private UserRole userRole;

  private Set<UserRole> userRoleList;

  private Integer version;

  private Boolean isDeprecated;

  public Role getRole() {
    return this.role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Set<Role> getRoleList() {
    return this.roleList;
  }

  public void setRoleList(Set<Role> roleList) {
    this.roleList = roleList;
  }

  public User getUser() {
    return this.user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Set<User> getUserList() {
    return this.userList;
  }

  public void setUserList(Set<User> userList) {
    this.userList = userList;
  }

  public Organization getOrganization() {
    return this.organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Set<Organization> getOrganizationList() {
    return this.organizationList;
  }

  public void setOrganizationList(Set<Organization> organizationList) {
    this.organizationList = organizationList;
  }

  public UserRole getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRole userRole) {
    this.userRole = userRole;
  }

  public Set<UserRole> getUserRoleList() {
    return this.userRoleList;
  }

  public void setUserRoleList(Set<UserRole> userRoleList) {
    this.userRoleList = userRoleList;
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
}
