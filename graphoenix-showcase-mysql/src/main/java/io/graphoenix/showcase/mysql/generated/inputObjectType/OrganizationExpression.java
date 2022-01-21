package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OrganizationExpression {
  private IDExpression id;

  private IntExpression aboveId;

  private OrganizationExpression above;

  private UserExpression users;

  private StringExpression name;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<OrganizationExpression> exs;

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

  public Collection<OrganizationExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<OrganizationExpression> exs) {
    this.exs = exs;
  }
}
