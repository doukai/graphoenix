package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OrganizationExpression {
  private Collection<OrganizationExpression> exs;

  private IntExpression aboveId;

  @DefaultValue("AND")
  private Conditional cond;

  private IntExpression version;

  private OrganizationExpression above;

  private UserExpression users;

  private IDExpression id;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression name;

  public Collection<OrganizationExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<OrganizationExpression> exs) {
    this.exs = exs;
  }

  public IntExpression getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(IntExpression aboveId) {
    this.aboveId = aboveId;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
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

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }
}
