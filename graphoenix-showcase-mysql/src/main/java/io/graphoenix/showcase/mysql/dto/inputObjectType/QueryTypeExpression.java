package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class QueryTypeExpression {
  private UserExpression user;

  private UserRoleExpression userRole;

  private __DirectiveLocationsExpression __directiveLocations;

  private __DirectiveLocationsExpression __directiveLocationsList;

  private __SchemaExpression __schema;

  private __EnumValueExpression __enumValue;

  private __SchemaExpression __schemaList;

  private __TypeInterfacesExpression __typeInterfaces;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private UserExpression userList;

  private RoleExpression role;

  private OrganizationExpression organization;

  private __TypePossibleTypesExpression __typePossibleTypes;

  @DefaultValue("AND")
  private Conditional cond;

  private __TypeExpression __type;

  private UserPhonesExpression userPhonesList;

  private __FieldExpression __fieldList;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private UserRoleExpression userRoleList;

  private __DirectiveExpression __directive;

  private OrganizationExpression organizationList;

  private __DirectiveExpression __directiveList;

  private UserPhonesExpression userPhones;

  private IntExpression version;

  private RoleExpression roleList;

  private __InputValueExpression __inputValue;

  private __TypePossibleTypesExpression __typePossibleTypesList;

  private IntExpression version;

  private __EnumValueExpression __enumValueList;

  private __InputValueExpression __inputValueList;

  private __TypeInterfacesExpression __typeInterfacesList;

  private Collection<QueryTypeExpression> exs;

  private __FieldExpression __field;

  private __TypeExpression __typeList;

  public UserExpression getUser() {
    return this.user;
  }

  public void setUser(UserExpression user) {
    this.user = user;
  }

  public UserRoleExpression getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRoleExpression userRole) {
    this.userRole = userRole;
  }

  public __DirectiveLocationsExpression get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(__DirectiveLocationsExpression __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }

  public __DirectiveLocationsExpression get__directiveLocationsList() {
    return this.__directiveLocationsList;
  }

  public void set__directiveLocationsList(__DirectiveLocationsExpression __directiveLocationsList) {
    this.__directiveLocationsList = __directiveLocationsList;
  }

  public __SchemaExpression get__schema() {
    return this.__schema;
  }

  public void set__schema(__SchemaExpression __schema) {
    this.__schema = __schema;
  }

  public __EnumValueExpression get__enumValue() {
    return this.__enumValue;
  }

  public void set__enumValue(__EnumValueExpression __enumValue) {
    this.__enumValue = __enumValue;
  }

  public __SchemaExpression get__schemaList() {
    return this.__schemaList;
  }

  public void set__schemaList(__SchemaExpression __schemaList) {
    this.__schemaList = __schemaList;
  }

  public __TypeInterfacesExpression get__typeInterfaces() {
    return this.__typeInterfaces;
  }

  public void set__typeInterfaces(__TypeInterfacesExpression __typeInterfaces) {
    this.__typeInterfaces = __typeInterfaces;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public UserExpression getUserList() {
    return this.userList;
  }

  public void setUserList(UserExpression userList) {
    this.userList = userList;
  }

  public RoleExpression getRole() {
    return this.role;
  }

  public void setRole(RoleExpression role) {
    this.role = role;
  }

  public OrganizationExpression getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationExpression organization) {
    this.organization = organization;
  }

  public __TypePossibleTypesExpression get__typePossibleTypes() {
    return this.__typePossibleTypes;
  }

  public void set__typePossibleTypes(__TypePossibleTypesExpression __typePossibleTypes) {
    this.__typePossibleTypes = __typePossibleTypes;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public __TypeExpression get__type() {
    return this.__type;
  }

  public void set__type(__TypeExpression __type) {
    this.__type = __type;
  }

  public UserPhonesExpression getUserPhonesList() {
    return this.userPhonesList;
  }

  public void setUserPhonesList(UserPhonesExpression userPhonesList) {
    this.userPhonesList = userPhonesList;
  }

  public __FieldExpression get__fieldList() {
    return this.__fieldList;
  }

  public void set__fieldList(__FieldExpression __fieldList) {
    this.__fieldList = __fieldList;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public UserRoleExpression getUserRoleList() {
    return this.userRoleList;
  }

  public void setUserRoleList(UserRoleExpression userRoleList) {
    this.userRoleList = userRoleList;
  }

  public __DirectiveExpression get__directive() {
    return this.__directive;
  }

  public void set__directive(__DirectiveExpression __directive) {
    this.__directive = __directive;
  }

  public OrganizationExpression getOrganizationList() {
    return this.organizationList;
  }

  public void setOrganizationList(OrganizationExpression organizationList) {
    this.organizationList = organizationList;
  }

  public __DirectiveExpression get__directiveList() {
    return this.__directiveList;
  }

  public void set__directiveList(__DirectiveExpression __directiveList) {
    this.__directiveList = __directiveList;
  }

  public UserPhonesExpression getUserPhones() {
    return this.userPhones;
  }

  public void setUserPhones(UserPhonesExpression userPhones) {
    this.userPhones = userPhones;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public RoleExpression getRoleList() {
    return this.roleList;
  }

  public void setRoleList(RoleExpression roleList) {
    this.roleList = roleList;
  }

  public __InputValueExpression get__inputValue() {
    return this.__inputValue;
  }

  public void set__inputValue(__InputValueExpression __inputValue) {
    this.__inputValue = __inputValue;
  }

  public __TypePossibleTypesExpression get__typePossibleTypesList() {
    return this.__typePossibleTypesList;
  }

  public void set__typePossibleTypesList(__TypePossibleTypesExpression __typePossibleTypesList) {
    this.__typePossibleTypesList = __typePossibleTypesList;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public __EnumValueExpression get__enumValueList() {
    return this.__enumValueList;
  }

  public void set__enumValueList(__EnumValueExpression __enumValueList) {
    this.__enumValueList = __enumValueList;
  }

  public __InputValueExpression get__inputValueList() {
    return this.__inputValueList;
  }

  public void set__inputValueList(__InputValueExpression __inputValueList) {
    this.__inputValueList = __inputValueList;
  }

  public __TypeInterfacesExpression get__typeInterfacesList() {
    return this.__typeInterfacesList;
  }

  public void set__typeInterfacesList(__TypeInterfacesExpression __typeInterfacesList) {
    this.__typeInterfacesList = __typeInterfacesList;
  }

  public Collection<QueryTypeExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<QueryTypeExpression> exs) {
    this.exs = exs;
  }

  public __FieldExpression get__field() {
    return this.__field;
  }

  public void set__field(__FieldExpression __field) {
    this.__field = __field;
  }

  public __TypeExpression get__typeList() {
    return this.__typeList;
  }

  public void set__typeList(__TypeExpression __typeList) {
    this.__typeList = __typeList;
  }
}
