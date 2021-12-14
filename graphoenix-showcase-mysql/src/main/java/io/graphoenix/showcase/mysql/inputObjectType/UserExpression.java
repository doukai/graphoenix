package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Conditional;
import java.lang.Boolean;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserExpression {
  private IDExpression id;

  private StringExpression login;

  private StringExpression password;

  private StringExpression name;

  private IntExpression age;

  private Boolean disable;

  private SexExpression sex;

  private IntExpression organizationId;

  private OrganizationExpression organization;

  private RoleExpression roles;

  private StringExpression phones;

  @DefaultValue("=AND")
  private Conditional cond;

  private Set<UserExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public StringExpression getLogin() {
    return this.login;
  }

  public void setLogin(StringExpression login) {
    this.login = login;
  }

  public StringExpression getPassword() {
    return this.password;
  }

  public void setPassword(StringExpression password) {
    this.password = password;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
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

  public SexExpression getSex() {
    return this.sex;
  }

  public void setSex(SexExpression sex) {
    this.sex = sex;
  }

  public IntExpression getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(IntExpression organizationId) {
    this.organizationId = organizationId;
  }

  public OrganizationExpression getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationExpression organization) {
    this.organization = organization;
  }

  public RoleExpression getRoles() {
    return this.roles;
  }

  public void setRoles(RoleExpression roles) {
    this.roles = roles;
  }

  public StringExpression getPhones() {
    return this.phones;
  }

  public void setPhones(StringExpression phones) {
    this.phones = phones;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Set<UserExpression> getExs() {
    return this.exs;
  }

  public void setExs(Set<UserExpression> exs) {
    this.exs = exs;
  }
}
