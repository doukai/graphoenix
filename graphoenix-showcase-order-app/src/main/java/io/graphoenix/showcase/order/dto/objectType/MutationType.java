package io.graphoenix.showcase.order.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.order.dto.interfaceType.Meta;
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
public class MutationType implements Meta {
  private User user;

  private Collection<User> userList;

  private UserProfile userProfile;

  private Collection<UserProfile> userProfileList;

  private UserMobileNumbers userMobileNumbers;

  private Collection<UserMobileNumbers> userMobileNumbersList;

  private UserRole userRole;

  private Collection<UserRole> userRoleList;

  private Role role;

  private Collection<Role> roleList;

  private RoleRoleType roleRoleType;

  private Collection<RoleRoleType> roleRoleTypeList;

  private Organization organization;

  private Collection<Organization> organizationList;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private Order order;

  private Collection<Order> orderList;

  private Good good;

  private Collection<Good> goodList;

  private Product product;

  private Collection<Product> productList;

  private Merchant merchant;

  private Collection<Merchant> merchantList;

  private __Schema __schema;

  private Collection<__Schema> __schemaList;

  private __Type __type;

  private Collection<__Type> __typeList;

  private __TypeInterfaces __typeInterfaces;

  private Collection<__TypeInterfaces> __typeInterfacesList;

  private __TypePossibleTypes __typePossibleTypes;

  private Collection<__TypePossibleTypes> __typePossibleTypesList;

  private __Field __field;

  private Collection<__Field> __fieldList;

  private __InputValue __inputValue;

  private Collection<__InputValue> __inputValueList;

  private __EnumValue __enumValue;

  private Collection<__EnumValue> __enumValueList;

  private __Directive __directive;

  private Collection<__Directive> __directiveList;

  private __DirectiveLocations __directiveLocations;

  private Collection<__DirectiveLocations> __directiveLocationsList;

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

  public Order getOrder() {
    return this.order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Collection<Order> getOrderList() {
    return this.orderList;
  }

  public void setOrderList(Collection<Order> orderList) {
    this.orderList = orderList;
  }

  public Good getGood() {
    return this.good;
  }

  public void setGood(Good good) {
    this.good = good;
  }

  public Collection<Good> getGoodList() {
    return this.goodList;
  }

  public void setGoodList(Collection<Good> goodList) {
    this.goodList = goodList;
  }

  public Product getProduct() {
    return this.product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Collection<Product> getProductList() {
    return this.productList;
  }

  public void setProductList(Collection<Product> productList) {
    this.productList = productList;
  }

  public Merchant getMerchant() {
    return this.merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public Collection<Merchant> getMerchantList() {
    return this.merchantList;
  }

  public void setMerchantList(Collection<Merchant> merchantList) {
    this.merchantList = merchantList;
  }

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
}
