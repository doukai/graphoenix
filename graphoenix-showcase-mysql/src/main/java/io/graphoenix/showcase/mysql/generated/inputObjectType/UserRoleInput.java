package io.graphoenix.showcase.mysql.generated.inputObjectType;

import java.lang.Boolean;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserRoleInput {
  private Integer id;

  private Integer userId;

  private Integer roleId;

  private Integer version;

  private Boolean isDeprecated;

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getRoleId() {
    return this.roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
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