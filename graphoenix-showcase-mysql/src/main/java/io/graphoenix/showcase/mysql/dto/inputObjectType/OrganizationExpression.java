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
public class OrganizationExpression {
  private StringExpression id;

  private IntExpression aboveId;

  private OrganizationExpression above;

  private UserExpression users;

  private StringExpression name;

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

  private Collection<OrganizationExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
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

  public Collection<OrganizationExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<OrganizationExpression> exs) {
    this.exs = exs;
  }
}
