package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class RoleExpression {
  private IDExpression id;

  private StringExpression name;

  private UserExpression users;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<RoleExpression> exs;

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

  public Collection<RoleExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<RoleExpression> exs) {
    this.exs = exs;
  }
}
