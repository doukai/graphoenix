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
  private Collection<UserPhonesExpression> exs;

  private IDExpression id;

  private IntExpression userId;

  @DefaultValue("AND")
  private Conditional cond;

  private StringExpression phone;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression version;

  public Collection<UserPhonesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserPhonesExpression> exs) {
    this.exs = exs;
  }

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

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public StringExpression getPhone() {
    return this.phone;
  }

  public void setPhone(StringExpression phone) {
    this.phone = phone;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }
}
