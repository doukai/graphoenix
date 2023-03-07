package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import null.dto.enumType.Sex;
import null.dto.objectType.Organization;
import null.dto.objectType.Role;
import null.dto.objectType.RoleConnection;
import null.dto.objectType.UserProfile;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class User implements Meta {
  @Id
  private String id;

  @NonNull
  private String login;

  @NonNull
  private String password;

  @NonNull
  private String name;

  private Integer age;

  private Boolean disable;

  private Sex sex;

  private UserProfile userProfile;

  private UserProfile rpcUserProfile;

  private Integer organizationId;

  private Organization rpcOrganization;

  private Organization organization;

  private Collection<Role> roles;

  private Collection<String> phones;

  private Collection<Integer> test1;

  private Collection<Boolean> test2;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer loginCount;

  private String loginMax;

  private String loginMin;

  private Integer passwordCount;

  private String passwordMax;

  private String passwordMin;

  private Integer nameCount;

  private String nameMax;

  private String nameMin;

  private Integer ageCount;

  private Integer ageSum;

  private Integer ageAvg;

  private Integer ageMax;

  private Integer ageMin;

  private Integer organizationIdCount;

  private Integer organizationIdSum;

  private Integer organizationIdAvg;

  private Integer organizationIdMax;

  private Integer organizationIdMin;

  private Role rolesAggregate;

  private RoleConnection rolesConnection;

  private String userDetail2;

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

  public Boolean getDisable() {
    return this.disable;
  }

  public void setDisable(Boolean disable) {
    this.disable = disable;
  }

  public Sex getSex() {
    return this.sex;
  }

  public void setSex(Sex sex) {
    this.sex = sex;
  }

  public UserProfile getUserProfile() {
    return this.userProfile;
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }

  public UserProfile getRpcUserProfile() {
    return this.rpcUserProfile;
  }

  public void setRpcUserProfile(UserProfile rpcUserProfile) {
    this.rpcUserProfile = rpcUserProfile;
  }

  public Integer getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(Integer organizationId) {
    this.organizationId = organizationId;
  }

  public Organization getRpcOrganization() {
    return this.rpcOrganization;
  }

  public void setRpcOrganization(Organization rpcOrganization) {
    this.rpcOrganization = rpcOrganization;
  }

  public Organization getOrganization() {
    return this.organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Collection<Role> getRoles() {
    return this.roles;
  }

  public void setRoles(Collection<Role> roles) {
    this.roles = roles;
  }

  public Collection<String> getPhones() {
    return this.phones;
  }

  public void setPhones(Collection<String> phones) {
    this.phones = phones;
  }

  public Collection<Integer> getTest1() {
    return this.test1;
  }

  public void setTest1(Collection<Integer> test1) {
    this.test1 = test1;
  }

  public Collection<Boolean> getTest2() {
    return this.test2;
  }

  public void setTest2(Collection<Boolean> test2) {
    this.test2 = test2;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String getRealmId() {
    return this.realmId;
  }

  @Override
  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  @Override
  public String getCreateUserId() {
    return this.createUserId;
  }

  @Override
  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  @Override
  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  @Override
  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  @Override
  public String getUpdateUserId() {
    return this.updateUserId;
  }

  @Override
  public void setUpdateUserId(String updateUserId) {
    this.updateUserId = updateUserId;
  }

  @Override
  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  @Override
  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public String getCreateGroupId() {
    return this.createGroupId;
  }

  @Override
  public void setCreateGroupId(String createGroupId) {
    this.createGroupId = createGroupId;
  }

  public String get__typename() {
    return this.__typename;
  }

  public void set__typename(String __typename) {
    this.__typename = __typename;
  }

  public Integer getIdCount() {
    return this.idCount;
  }

  public void setIdCount(Integer idCount) {
    this.idCount = idCount;
  }

  public String getIdMax() {
    return this.idMax;
  }

  public void setIdMax(String idMax) {
    this.idMax = idMax;
  }

  public String getIdMin() {
    return this.idMin;
  }

  public void setIdMin(String idMin) {
    this.idMin = idMin;
  }

  public Integer getLoginCount() {
    return this.loginCount;
  }

  public void setLoginCount(Integer loginCount) {
    this.loginCount = loginCount;
  }

  public String getLoginMax() {
    return this.loginMax;
  }

  public void setLoginMax(String loginMax) {
    this.loginMax = loginMax;
  }

  public String getLoginMin() {
    return this.loginMin;
  }

  public void setLoginMin(String loginMin) {
    this.loginMin = loginMin;
  }

  public Integer getPasswordCount() {
    return this.passwordCount;
  }

  public void setPasswordCount(Integer passwordCount) {
    this.passwordCount = passwordCount;
  }

  public String getPasswordMax() {
    return this.passwordMax;
  }

  public void setPasswordMax(String passwordMax) {
    this.passwordMax = passwordMax;
  }

  public String getPasswordMin() {
    return this.passwordMin;
  }

  public void setPasswordMin(String passwordMin) {
    this.passwordMin = passwordMin;
  }

  public Integer getNameCount() {
    return this.nameCount;
  }

  public void setNameCount(Integer nameCount) {
    this.nameCount = nameCount;
  }

  public String getNameMax() {
    return this.nameMax;
  }

  public void setNameMax(String nameMax) {
    this.nameMax = nameMax;
  }

  public String getNameMin() {
    return this.nameMin;
  }

  public void setNameMin(String nameMin) {
    this.nameMin = nameMin;
  }

  public Integer getAgeCount() {
    return this.ageCount;
  }

  public void setAgeCount(Integer ageCount) {
    this.ageCount = ageCount;
  }

  public Integer getAgeSum() {
    return this.ageSum;
  }

  public void setAgeSum(Integer ageSum) {
    this.ageSum = ageSum;
  }

  public Integer getAgeAvg() {
    return this.ageAvg;
  }

  public void setAgeAvg(Integer ageAvg) {
    this.ageAvg = ageAvg;
  }

  public Integer getAgeMax() {
    return this.ageMax;
  }

  public void setAgeMax(Integer ageMax) {
    this.ageMax = ageMax;
  }

  public Integer getAgeMin() {
    return this.ageMin;
  }

  public void setAgeMin(Integer ageMin) {
    this.ageMin = ageMin;
  }

  public Integer getOrganizationIdCount() {
    return this.organizationIdCount;
  }

  public void setOrganizationIdCount(Integer organizationIdCount) {
    this.organizationIdCount = organizationIdCount;
  }

  public Integer getOrganizationIdSum() {
    return this.organizationIdSum;
  }

  public void setOrganizationIdSum(Integer organizationIdSum) {
    this.organizationIdSum = organizationIdSum;
  }

  public Integer getOrganizationIdAvg() {
    return this.organizationIdAvg;
  }

  public void setOrganizationIdAvg(Integer organizationIdAvg) {
    this.organizationIdAvg = organizationIdAvg;
  }

  public Integer getOrganizationIdMax() {
    return this.organizationIdMax;
  }

  public void setOrganizationIdMax(Integer organizationIdMax) {
    this.organizationIdMax = organizationIdMax;
  }

  public Integer getOrganizationIdMin() {
    return this.organizationIdMin;
  }

  public void setOrganizationIdMin(Integer organizationIdMin) {
    this.organizationIdMin = organizationIdMin;
  }

  public Role getRolesAggregate() {
    return this.rolesAggregate;
  }

  public void setRolesAggregate(Role rolesAggregate) {
    this.rolesAggregate = rolesAggregate;
  }

  public RoleConnection getRolesConnection() {
    return this.rolesConnection;
  }

  public void setRolesConnection(RoleConnection rolesConnection) {
    this.rolesConnection = rolesConnection;
  }

  public String getUserDetail2() {
    return this.userDetail2;
  }

  public void setUserDetail2(String userDetail2) {
    this.userDetail2 = userDetail2;
  }
}
