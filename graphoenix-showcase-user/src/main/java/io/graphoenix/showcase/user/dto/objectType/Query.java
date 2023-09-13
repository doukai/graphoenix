package io.graphoenix.showcase.user.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Query implements Meta {
  private User user;

  private Collection<User> userList;

  private UserConnection userConnection;

  private UserProfile userProfile;

  private Collection<UserProfile> userProfileList;

  private UserProfileConnection userProfileConnection;

  private Role role;

  private Collection<Role> roleList;

  private RoleConnection roleConnection;

  private Organization organization;

  private Collection<Organization> organizationList;

  private OrganizationConnection organizationConnection;

  private UserRole userRole;

  private Collection<UserRole> userRoleList;

  private UserRoleConnection userRoleConnection;

  private UserMobileNumbers userMobileNumbers;

  private Collection<UserMobileNumbers> userMobileNumbersList;

  private UserMobileNumbersConnection userMobileNumbersConnection;

  private RoleRoleType roleRoleType;

  private Collection<RoleRoleType> roleRoleTypeList;

  private RoleRoleTypeConnection roleRoleTypeConnection;

  private Boolean isDeprecated;

  private Integer version;

  private Integer realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String login;

  private String metaInfo;

  public User getUser() {
    return this.user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Collection<User> getUserList() {
    return this.userList;
  }

  public void setUserList(Collection<User> userList) {
    this.userList = userList;
  }

  public UserConnection getUserConnection() {
    return this.userConnection;
  }

  public void setUserConnection(UserConnection userConnection) {
    this.userConnection = userConnection;
  }

  public UserProfile getUserProfile() {
    return this.userProfile;
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }

  public Collection<UserProfile> getUserProfileList() {
    return this.userProfileList;
  }

  public void setUserProfileList(Collection<UserProfile> userProfileList) {
    this.userProfileList = userProfileList;
  }

  public UserProfileConnection getUserProfileConnection() {
    return this.userProfileConnection;
  }

  public void setUserProfileConnection(UserProfileConnection userProfileConnection) {
    this.userProfileConnection = userProfileConnection;
  }

  public Role getRole() {
    return this.role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Collection<Role> getRoleList() {
    return this.roleList;
  }

  public void setRoleList(Collection<Role> roleList) {
    this.roleList = roleList;
  }

  public RoleConnection getRoleConnection() {
    return this.roleConnection;
  }

  public void setRoleConnection(RoleConnection roleConnection) {
    this.roleConnection = roleConnection;
  }

  public Organization getOrganization() {
    return this.organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Collection<Organization> getOrganizationList() {
    return this.organizationList;
  }

  public void setOrganizationList(Collection<Organization> organizationList) {
    this.organizationList = organizationList;
  }

  public OrganizationConnection getOrganizationConnection() {
    return this.organizationConnection;
  }

  public void setOrganizationConnection(OrganizationConnection organizationConnection) {
    this.organizationConnection = organizationConnection;
  }

  public UserRole getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRole userRole) {
    this.userRole = userRole;
  }

  public Collection<UserRole> getUserRoleList() {
    return this.userRoleList;
  }

  public void setUserRoleList(Collection<UserRole> userRoleList) {
    this.userRoleList = userRoleList;
  }

  public UserRoleConnection getUserRoleConnection() {
    return this.userRoleConnection;
  }

  public void setUserRoleConnection(UserRoleConnection userRoleConnection) {
    this.userRoleConnection = userRoleConnection;
  }

  public UserMobileNumbers getUserMobileNumbers() {
    return this.userMobileNumbers;
  }

  public void setUserMobileNumbers(UserMobileNumbers userMobileNumbers) {
    this.userMobileNumbers = userMobileNumbers;
  }

  public Collection<UserMobileNumbers> getUserMobileNumbersList() {
    return this.userMobileNumbersList;
  }

  public void setUserMobileNumbersList(Collection<UserMobileNumbers> userMobileNumbersList) {
    this.userMobileNumbersList = userMobileNumbersList;
  }

  public UserMobileNumbersConnection getUserMobileNumbersConnection() {
    return this.userMobileNumbersConnection;
  }

  public void setUserMobileNumbersConnection(
      UserMobileNumbersConnection userMobileNumbersConnection) {
    this.userMobileNumbersConnection = userMobileNumbersConnection;
  }

  public RoleRoleType getRoleRoleType() {
    return this.roleRoleType;
  }

  public void setRoleRoleType(RoleRoleType roleRoleType) {
    this.roleRoleType = roleRoleType;
  }

  public Collection<RoleRoleType> getRoleRoleTypeList() {
    return this.roleRoleTypeList;
  }

  public void setRoleRoleTypeList(Collection<RoleRoleType> roleRoleTypeList) {
    this.roleRoleTypeList = roleRoleTypeList;
  }

  public RoleRoleTypeConnection getRoleRoleTypeConnection() {
    return this.roleRoleTypeConnection;
  }

  public void setRoleRoleTypeConnection(RoleRoleTypeConnection roleRoleTypeConnection) {
    this.roleRoleTypeConnection = roleRoleTypeConnection;
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
  public Integer getRealmId() {
    return this.realmId;
  }

  @Override
  public void setRealmId(Integer realmId) {
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

  public String getLogin() {
    return this.login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getMetaInfo() {
    return this.metaInfo;
  }

  public void setMetaInfo(String metaInfo) {
    this.metaInfo = metaInfo;
  }
}
