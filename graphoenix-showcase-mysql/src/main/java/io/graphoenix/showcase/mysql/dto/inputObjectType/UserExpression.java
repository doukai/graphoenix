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
public class UserExpression {
  private StringExpression id;

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

  private IntExpression test1;

  private Boolean test2;

  private StringExpression domainId;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression version;

  private StringExpression createUserId;

  private StringExpression createTime;

  private StringExpression updateUserId;

  private StringExpression updateTime;

  private StringExpression createOrganizationId;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<UserExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
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

  public IntExpression getTest1() {
    return this.test1;
  }

  public void setTest1(IntExpression test1) {
    this.test1 = test1;
  }

  public Boolean getTest2() {
    return this.test2;
  }

  public void setTest2(Boolean test2) {
    this.test2 = test2;
  }

  public StringExpression getDomainId() {
    return this.domainId;
  }

  public void setDomainId(StringExpression domainId) {
    this.domainId = domainId;
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

  public StringExpression getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(StringExpression createUserId) {
    this.createUserId = createUserId;
  }

  public StringExpression getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(StringExpression createTime) {
    this.createTime = createTime;
  }

  public StringExpression getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(StringExpression updateUserId) {
    this.updateUserId = updateUserId;
  }

  public StringExpression getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(StringExpression updateTime) {
    this.updateTime = updateTime;
  }

  public StringExpression getCreateOrganizationId() {
    return this.createOrganizationId;
  }

  public void setCreateOrganizationId(StringExpression createOrganizationId) {
    this.createOrganizationId = createOrganizationId;
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

  public Collection<UserExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<UserExpression> exs) {
    this.exs = exs;
  }
}
