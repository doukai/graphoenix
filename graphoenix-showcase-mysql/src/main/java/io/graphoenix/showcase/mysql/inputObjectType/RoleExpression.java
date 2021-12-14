package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class RoleExpression {
  private IDExpression id;

  private StringExpression name;

  private UserExpression users;

  @DefaultValue("=AND")
  private Conditional cond;

  private Set<RoleExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public UserExpression getUsers() {
    return this.users;
  }

  public void setUsers(UserExpression users) {
    this.users = users;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Set<RoleExpression> getExs() {
    return this.exs;
  }

  public void setExs(Set<RoleExpression> exs) {
    this.exs = exs;
  }
}
