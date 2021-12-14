package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserRoleExpression {
  private IDExpression id;

  private IntExpression userId;

  private IntExpression roleId;

  @DefaultValue("=AND")
  private Conditional cond;

  private Set<UserRoleExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public IntExpression getUserId() {
    return this.userId;
  }

  public void setUserId(IntExpression userId) {
    this.userId = userId;
  }

  public IntExpression getRoleId() {
    return this.roleId;
  }

  public void setRoleId(IntExpression roleId) {
    this.roleId = roleId;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Set<UserRoleExpression> getExs() {
    return this.exs;
  }

  public void setExs(Set<UserRoleExpression> exs) {
    this.exs = exs;
  }
}
