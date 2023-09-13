package io.graphoenix.showcase.user.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Conditional;
import io.graphoenix.core.dto.inputObjectType.BooleanExpression;
import io.graphoenix.core.dto.inputObjectType.IntExpression;
import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Subscription_user_Arguments implements MetaExpression {
  private StringExpression id;

  private StringExpression login;

  private StringExpression password;

  private StringExpression name;

  private IntExpression age;

  private BooleanExpression disabled;

  private SexExpression sex;

  private UserProfileExpression userProfile;

  private OrganizationExpression organization;

  private RoleExpression roles;

  private StringExpression mobileNumbers;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression version;

  private IntExpression realmId;

  private StringExpression createUserId;

  private StringExpression createTime;

  private StringExpression updateUserId;

  private StringExpression updateTime;

  private StringExpression createGroupId;

  private StringExpression __typename;

  private IntExpression organizationId;

  private UserRoleExpression userRole;

  private UserMobileNumbersExpression userMobileNumbers;

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

  public BooleanExpression getDisabled() {
    return this.disabled;
  }

  public void setDisabled(BooleanExpression disabled) {
    this.disabled = disabled;
  }

  public SexExpression getSex() {
    return this.sex;
  }

  public void setSex(SexExpression sex) {
    this.sex = sex;
  }

  public UserProfileExpression getUserProfile() {
    return this.userProfile;
  }

  public void setUserProfile(UserProfileExpression userProfile) {
    this.userProfile = userProfile;
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

  public StringExpression getMobileNumbers() {
    return this.mobileNumbers;
  }

  public void setMobileNumbers(StringExpression mobileNumbers) {
    this.mobileNumbers = mobileNumbers;
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

  public IntExpression getRealmId() {
    return this.realmId;
  }

  public void setRealmId(IntExpression realmId) {
    this.realmId = realmId;
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

  public StringExpression getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(StringExpression createGroupId) {
    this.createGroupId = createGroupId;
  }

  public StringExpression get__typename() {
    return this.__typename;
  }

  public void set__typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public IntExpression getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(IntExpression organizationId) {
    this.organizationId = organizationId;
  }

  public UserRoleExpression getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRoleExpression userRole) {
    this.userRole = userRole;
  }

  public UserMobileNumbersExpression getUserMobileNumbers() {
    return this.userMobileNumbers;
  }

  public void setUserMobileNumbers(UserMobileNumbersExpression userMobileNumbers) {
    this.userMobileNumbers = userMobileNumbers;
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
