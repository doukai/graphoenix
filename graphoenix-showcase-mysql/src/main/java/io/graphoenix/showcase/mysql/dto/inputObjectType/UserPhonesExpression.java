package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class UserPhonesExpression {
  private IDExpression id;

  private IntExpression userId;

  private StringExpression phone;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<UserPhonesExpression> exs;

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

  public StringExpression get__Typename() {
    return this.__typename;
  }

  public void set__Typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<UserPhonesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserPhonesExpression> exs) {
    this.exs = exs;
  }
}
