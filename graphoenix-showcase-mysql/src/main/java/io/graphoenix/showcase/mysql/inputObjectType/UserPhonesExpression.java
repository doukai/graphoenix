package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserPhonesExpression {
  private IDExpression id;

  private IntExpression userId;

  private StringExpression phone;

  @DefaultValue("=AND")
  private Conditional cond;

  private Set<UserPhonesExpression> exs;

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

  public StringExpression getPhone() {
    return this.phone;
  }

  public void setPhone(StringExpression phone) {
    this.phone = phone;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Set<UserPhonesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Set<UserPhonesExpression> exs) {
    this.exs = exs;
  }
}
