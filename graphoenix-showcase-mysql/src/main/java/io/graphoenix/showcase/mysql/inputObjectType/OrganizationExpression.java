package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class OrganizationExpression {
  private IDExpression id;

  private IntExpression aboveId;

  private OrganizationExpression above;

  private UserExpression users;

  private StringExpression name;

  @DefaultValue("=AND")
  private Conditional cond;

  private Set<OrganizationExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public IntExpression getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(IntExpression aboveId) {
    this.aboveId = aboveId;
  }

  public OrganizationExpression getAbove() {
    return this.above;
  }

  public void setAbove(OrganizationExpression above) {
    this.above = above;
  }

  public UserExpression getUsers() {
    return this.users;
  }

  public void setUsers(UserExpression users) {
    this.users = users;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Set<OrganizationExpression> getExs() {
    return this.exs;
  }

  public void setExs(Set<OrganizationExpression> exs) {
    this.exs = exs;
  }
}
