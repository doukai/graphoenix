package io.graphoenix.showcase.user.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.user.dto.enumType.Sex;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class UserMutationTypeArguments {
  private String id;

  private String login;

  private String password;

  private String name;

  private Integer age;

  private Boolean disabled;

  private Sex sex;

  private UserProfileInput userProfile;

  private OrganizationInput organization;

  private Collection<RoleInput> roles;

  private Collection<String> mobileNumbers;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  @DefaultValue("\"User\"")
  private String __typename;

  private Integer organizationId;

  private Collection<UserRoleInput> userRole;

  private Collection<UserMobileNumbersInput> userMobileNumbers;

  private UserExpression where;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLogin() {
    return this.login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getAge() {
    return this.age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public Boolean getDisabled() {
    return this.disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  public Sex getSex() {
    return this.sex;
  }

  public void setSex(Sex sex) {
    this.sex = sex;
  }

  public UserProfileInput getUserProfile() {
    return this.userProfile;
  }

  public void setUserProfile(UserProfileInput userProfile) {
    this.userProfile = userProfile;
  }

  public OrganizationInput getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationInput organization) {
    this.organization = organization;
  }

  public Collection<RoleInput> getRoles() {
    return this.roles;
  }

  public void setRoles(Collection<RoleInput> roles) {
    this.roles = roles;
  }

  public Collection<String> getMobileNumbers() {
    return this.mobileNumbers;
  }

  public void setMobileNumbers(Collection<String> mobileNumbers) {
    this.mobileNumbers = mobileNumbers;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getRealmId() {
    return this.realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public String getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public String getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(String updateUserId) {
    this.updateUserId = updateUserId;
  }

  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public String getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(String createGroupId) {
    this.createGroupId = createGroupId;
  }

  public String get__typename() {
    return this.__typename;
  }

  public void set__typename(String __typename) {
    this.__typename = __typename;
  }

  public Integer getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(Integer organizationId) {
    this.organizationId = organizationId;
  }

  public Collection<UserRoleInput> getUserRole() {
    return this.userRole;
  }

  public void setUserRole(Collection<UserRoleInput> userRole) {
    this.userRole = userRole;
  }

  public Collection<UserMobileNumbersInput> getUserMobileNumbers() {
    return this.userMobileNumbers;
  }

  public void setUserMobileNumbers(Collection<UserMobileNumbersInput> userMobileNumbers) {
    this.userMobileNumbers = userMobileNumbers;
  }

  public UserExpression getWhere() {
    return this.where;
  }

  public void setWhere(UserExpression where) {
    this.where = where;
  }
}
