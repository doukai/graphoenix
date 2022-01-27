package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserPhonesExpression {
  private StringExpression phone;

  private IntExpression version;

  @DefaultValue("AND")
  private Conditional cond;

  private IDExpression id;

  private Collection<UserPhonesExpression> exs;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression userId;

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

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public Collection<UserPhonesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserPhonesExpression> exs) {
    this.exs = exs;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IntExpression getUserId() {
    return this.userId;
  }

  public void setUserId(IntExpression userId) {
    this.userId = userId;
  }
}
