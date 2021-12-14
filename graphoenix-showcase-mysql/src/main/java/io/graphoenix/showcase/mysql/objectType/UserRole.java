package io.graphoenix.showcase.mysql.objectType;

import io.graphoenix.showcase.mysql.interfaceType.Meta;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
public class UserRole implements Meta {
  @Id
  private Integer id;

  private Integer userId;

  private Integer roleId;

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
}
