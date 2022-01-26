package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserExpression {
  private StringExpression phones;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression age;

  private Boolean disable;

  private StringExpression password;

  private IDExpression id;

  private IntExpression version;

  private RoleExpression roles;

  private IntExpression organizationId;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<UserExpression> exs;

  private StringExpression name;

  private StringExpression login;

  private SexExpression sex;

  private OrganizationExpression organization;

  public StringExpression getPhones() {
    return this.phones;
  }

  public void setPhones(StringExpression phones) {
    this.phones = phones;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IntExpression getAge() {
    return this.age;
  }

  public void setAge(IntExpression age) {
    this.age = age;
  }

  public Boolean getDisable() {
    return this.disable;
  }

  public void setDisable(Boolean disable) {
    this.disable = disable;
  }

  public StringExpression getPassword() {
    return this.password;
  }

  public void setPassword(StringExpression password) {
    this.password = password;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public RoleExpression getRoles() {
    return this.roles;
  }

  public void setRoles(RoleExpression roles) {
    this.roles = roles;
  }

  public IntExpression getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(IntExpression organizationId) {
    this.organizationId = organizationId;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<UserExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserExpression> exs) {
    this.exs = exs;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public StringExpression getLogin() {
    return this.login;
  }

  public void setLogin(StringExpression login) {
    this.login = login;
  }

  public SexExpression getSex() {
    return this.sex;
  }

  public void setSex(SexExpression sex) {
    this.sex = sex;
  }

  public OrganizationExpression getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationExpression organization) {
    this.organization = organization;
  }
}
