package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Conditional;
import java.lang.Boolean;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserPhonesExpression {
  private IDExpression id;

  private IntExpression userId;

  private StringExpression phone;

  private IntExpression version;

  @DefaultValue("=false")
  private Boolean includeDeprecated;

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

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
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
