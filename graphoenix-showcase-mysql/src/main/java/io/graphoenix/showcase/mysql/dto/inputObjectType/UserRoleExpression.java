package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserRoleExpression {
  private IntExpression userId;

  private IDExpression id;

  private Collection<UserRoleExpression> exs;

  private IntExpression version;

  @DefaultValue("AND")
  private Conditional cond;

  private IntExpression roleId;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  public IntExpression getUserId() {
    return this.userId;
  }

  public void setUserId(IntExpression userId) {
    this.userId = userId;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public Collection<UserRoleExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserRoleExpression> exs) {
    this.exs = exs;
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

  public IntExpression getRoleId() {
    return this.roleId;
  }

  public void setRoleId(IntExpression roleId) {
    this.roleId = roleId;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }
}
