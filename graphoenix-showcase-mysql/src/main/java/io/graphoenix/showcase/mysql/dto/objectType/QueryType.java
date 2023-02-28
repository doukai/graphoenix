package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.api.ContainerType;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
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
public class QueryType implements Meta {
  private __Schema __schema;

  private Collection<__Schema> __schemaList;

  private __SchemaConnection __schemaConnection;

  private __Type __type;

  private Collection<__Type> __typeList;

  private __TypeConnection __typeConnection;

  private __TypeInterfaces __typeInterfaces;

  private Collection<__TypeInterfaces> __typeInterfacesList;

  private __TypeInterfacesConnection __typeInterfacesConnection;

  private __TypePossibleTypes __typePossibleTypes;

  private Collection<__TypePossibleTypes> __typePossibleTypesList;

  private __TypePossibleTypesConnection __typePossibleTypesConnection;

  private __Field __field;

  private Collection<__Field> __fieldList;

  private __FieldConnection __fieldConnection;

  private __InputValue __inputValue;

  private Collection<__InputValue> __inputValueList;

  private __InputValueConnection __inputValueConnection;

  private __EnumValue __enumValue;

  private Collection<__EnumValue> __enumValueList;

  private __EnumValueConnection __enumValueConnection;

  private __Directive __directive;

  private Collection<__Directive> __directiveList;

  private __DirectiveConnection __directiveConnection;

  private __DirectiveLocations __directiveLocations;

  private Collection<__DirectiveLocations> __directiveLocationsList;

  private __DirectiveLocationsConnection __directiveLocationsConnection;

  private User user;

  private Collection<User> userList;

  private UserConnection userConnection;

  private UserProfile userProfile;

  private Collection<UserProfile> userProfileList;

  private UserProfileConnection userProfileConnection;

  private UserPhones userPhones;

  private Collection<UserPhones> userPhonesList;

  private UserPhonesConnection userPhonesConnection;

  private UserTest1 userTest1;

  private Collection<UserTest1> userTest1List;

  private UserTest1Connection userTest1Connection;

  private UserTest2 userTest2;

  private Collection<UserTest2> userTest2List;

  private UserTest2Connection userTest2Connection;

  private UserRole userRole;

  private Collection<UserRole> userRoleList;

  private UserRoleConnection userRoleConnection;

  private Role role;

  private Collection<Role> roleList;

  private RoleConnection roleConnection;

  private RoleRoleType roleRoleType;

  private Collection<RoleRoleType> roleRoleTypeList;

  private RoleRoleTypeConnection roleRoleTypeConnection;

  private Organization organization;

  private Collection<Organization> organizationList;

  private OrganizationConnection organizationConnection;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String userDetail;

  private Collection<String> userDetail6;

  private Integer orgLevel;

  private Collection<Boolean> roleDisable2;

  private Collection<Integer> orgLevel5;

  private Collection<User> userByOrg2;

  private Organization parent2;

  private Collection<Role> findRole2;

  private ContainerType findContainerType;

  public __Schema get__schema() {
    return this.__schema;
  }

  public void set__schema(__Schema __schema) {
    this.__schema = __schema;
  }

  public Collection<__Schema> get__schemaList() {
    return this.__schemaList;
  }

  public void set__schemaList(Collection<__Schema> __schemaList) {
    this.__schemaList = __schemaList;
  }

  public __SchemaConnection get__schemaConnection() {
    return this.__schemaConnection;
  }

  public void set__schemaConnection(__SchemaConnection __schemaConnection) {
    this.__schemaConnection = __schemaConnection;
  }

  public __Type get__type() {
    return this.__type;
  }

  public void set__type(__Type __type) {
    this.__type = __type;
  }

  public Collection<__Type> get__typeList() {
    return this.__typeList;
  }

  public void set__typeList(Collection<__Type> __typeList) {
    this.__typeList = __typeList;
  }

  public __TypeConnection get__typeConnection() {
    return this.__typeConnection;
  }

  public void set__typeConnection(__TypeConnection __typeConnection) {
    this.__typeConnection = __typeConnection;
  }

  public __TypeInterfaces get__typeInterfaces() {
    return this.__typeInterfaces;
  }

  public void set__typeInterfaces(__TypeInterfaces __typeInterfaces) {
    this.__typeInterfaces = __typeInterfaces;
  }

  public Collection<__TypeInterfaces> get__typeInterfacesList() {
    return this.__typeInterfacesList;
  }

  public void set__typeInterfacesList(Collection<__TypeInterfaces> __typeInterfacesList) {
    this.__typeInterfacesList = __typeInterfacesList;
  }

  public __TypeInterfacesConnection get__typeInterfacesConnection() {
    return this.__typeInterfacesConnection;
  }

  public void set__typeInterfacesConnection(__TypeInterfacesConnection __typeInterfacesConnection) {
    this.__typeInterfacesConnection = __typeInterfacesConnection;
  }

  public __TypePossibleTypes get__typePossibleTypes() {
    return this.__typePossibleTypes;
  }

  public void set__typePossibleTypes(__TypePossibleTypes __typePossibleTypes) {
    this.__typePossibleTypes = __typePossibleTypes;
  }

  public Collection<__TypePossibleTypes> get__typePossibleTypesList() {
    return this.__typePossibleTypesList;
  }

  public void set__typePossibleTypesList(Collection<__TypePossibleTypes> __typePossibleTypesList) {
    this.__typePossibleTypesList = __typePossibleTypesList;
  }

  public __TypePossibleTypesConnection get__typePossibleTypesConnection() {
    return this.__typePossibleTypesConnection;
  }

  public void set__typePossibleTypesConnection(
      __TypePossibleTypesConnection __typePossibleTypesConnection) {
    this.__typePossibleTypesConnection = __typePossibleTypesConnection;
  }

  public __Field get__field() {
    return this.__field;
  }

  public void set__field(__Field __field) {
    this.__field = __field;
  }

  public Collection<__Field> get__fieldList() {
    return this.__fieldList;
  }

  public void set__fieldList(Collection<__Field> __fieldList) {
    this.__fieldList = __fieldList;
  }

  public __FieldConnection get__fieldConnection() {
    return this.__fieldConnection;
  }

  public void set__fieldConnection(__FieldConnection __fieldConnection) {
    this.__fieldConnection = __fieldConnection;
  }

  public __InputValue get__inputValue() {
    return this.__inputValue;
  }

  public void set__inputValue(__InputValue __inputValue) {
    this.__inputValue = __inputValue;
  }

  public Collection<__InputValue> get__inputValueList() {
    return this.__inputValueList;
  }

  public void set__inputValueList(Collection<__InputValue> __inputValueList) {
    this.__inputValueList = __inputValueList;
  }

  public __InputValueConnection get__inputValueConnection() {
    return this.__inputValueConnection;
  }

  public void set__inputValueConnection(__InputValueConnection __inputValueConnection) {
    this.__inputValueConnection = __inputValueConnection;
  }

  public __EnumValue get__enumValue() {
    return this.__enumValue;
  }

  public void set__enumValue(__EnumValue __enumValue) {
    this.__enumValue = __enumValue;
  }

  public Collection<__EnumValue> get__enumValueList() {
    return this.__enumValueList;
  }

  public void set__enumValueList(Collection<__EnumValue> __enumValueList) {
    this.__enumValueList = __enumValueList;
  }

  public __EnumValueConnection get__enumValueConnection() {
    return this.__enumValueConnection;
  }

  public void set__enumValueConnection(__EnumValueConnection __enumValueConnection) {
    this.__enumValueConnection = __enumValueConnection;
  }

  public __Directive get__directive() {
    return this.__directive;
  }

  public void set__directive(__Directive __directive) {
    this.__directive = __directive;
  }

  public Collection<__Directive> get__directiveList() {
    return this.__directiveList;
  }

  public void set__directiveList(Collection<__Directive> __directiveList) {
    this.__directiveList = __directiveList;
  }

  public __DirectiveConnection get__directiveConnection() {
    return this.__directiveConnection;
  }

  public void set__directiveConnection(__DirectiveConnection __directiveConnection) {
    this.__directiveConnection = __directiveConnection;
  }

  public __DirectiveLocations get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(__DirectiveLocations __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }

  public Collection<__DirectiveLocations> get__directiveLocationsList() {
    return this.__directiveLocationsList;
  }

  public void set__directiveLocationsList(
      Collection<__DirectiveLocations> __directiveLocationsList) {
    this.__directiveLocationsList = __directiveLocationsList;
  }

  public __DirectiveLocationsConnection get__directiveLocationsConnection() {
    return this.__directiveLocationsConnection;
  }

  public void set__directiveLocationsConnection(
      __DirectiveLocationsConnection __directiveLocationsConnection) {
    this.__directiveLocationsConnection = __directiveLocationsConnection;
  }

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

  public UserPhones getUserPhones() {
    return this.userPhones;
  }

  public void setUserPhones(UserPhones userPhones) {
    this.userPhones = userPhones;
  }

  public Collection<UserPhones> getUserPhonesList() {
    return this.userPhonesList;
  }

  public void setUserPhonesList(Collection<UserPhones> userPhonesList) {
    this.userPhonesList = userPhonesList;
  }

  public UserPhonesConnection getUserPhonesConnection() {
    return this.userPhonesConnection;
  }

  public void setUserPhonesConnection(UserPhonesConnection userPhonesConnection) {
    this.userPhonesConnection = userPhonesConnection;
  }

  public UserTest1 getUserTest1() {
    return this.userTest1;
  }

  public void setUserTest1(UserTest1 userTest1) {
    this.userTest1 = userTest1;
  }

  public Collection<UserTest1> getUserTest1List() {
    return this.userTest1List;
  }

  public void setUserTest1List(Collection<UserTest1> userTest1List) {
    this.userTest1List = userTest1List;
  }

  public UserTest1Connection getUserTest1Connection() {
    return this.userTest1Connection;
  }

  public void setUserTest1Connection(UserTest1Connection userTest1Connection) {
    this.userTest1Connection = userTest1Connection;
  }

  public UserTest2 getUserTest2() {
    return this.userTest2;
  }

  public void setUserTest2(UserTest2 userTest2) {
    this.userTest2 = userTest2;
  }

  public Collection<UserTest2> getUserTest2List() {
    return this.userTest2List;
  }

  public void setUserTest2List(Collection<UserTest2> userTest2List) {
    this.userTest2List = userTest2List;
  }

  public UserTest2Connection getUserTest2Connection() {
    return this.userTest2Connection;
  }

  public void setUserTest2Connection(UserTest2Connection userTest2Connection) {
    this.userTest2Connection = userTest2Connection;
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

  public String getUserDetail() {
    return this.userDetail;
  }

  public void setUserDetail(String userDetail) {
    this.userDetail = userDetail;
  }

  public Collection<String> getUserDetail6() {
    return this.userDetail6;
  }

  public void setUserDetail6(Collection<String> userDetail6) {
    this.userDetail6 = userDetail6;
  }

  public Integer getOrgLevel() {
    return this.orgLevel;
  }

  public void setOrgLevel(Integer orgLevel) {
    this.orgLevel = orgLevel;
  }

  public Collection<Boolean> getRoleDisable2() {
    return this.roleDisable2;
  }

  public void setRoleDisable2(Collection<Boolean> roleDisable2) {
    this.roleDisable2 = roleDisable2;
  }

  public Collection<Integer> getOrgLevel5() {
    return this.orgLevel5;
  }

  public void setOrgLevel5(Collection<Integer> orgLevel5) {
    this.orgLevel5 = orgLevel5;
  }

  public Collection<User> getUserByOrg2() {
    return this.userByOrg2;
  }

  public void setUserByOrg2(Collection<User> userByOrg2) {
    this.userByOrg2 = userByOrg2;
  }

  public Organization getParent2() {
    return this.parent2;
  }

  public void setParent2(Organization parent2) {
    this.parent2 = parent2;
  }

  public Collection<Role> getFindRole2() {
    return this.findRole2;
  }

  public void setFindRole2(Collection<Role> findRole2) {
    this.findRole2 = findRole2;
  }

  public ContainerType getFindContainerType() {
    return this.findContainerType;
  }

  public void setFindContainerType(ContainerType findContainerType) {
    this.findContainerType = findContainerType;
  }
}
