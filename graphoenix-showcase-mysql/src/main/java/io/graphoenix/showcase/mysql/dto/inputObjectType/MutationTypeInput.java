package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class MutationTypeInput {
  private UserPhonesInput userPhones;

  private __TypeInterfacesInput __typeInterfaces;

  private Boolean isDeprecated;

  private __DirectiveLocationsInput __directiveLocations;

  private __TypeInput __type;

  private __DirectiveInput __directive;

  private OrganizationInput organization;

  private __SchemaInput __schema;

  private Integer version;

  private UserInput user;

  private RoleInput role;

  private Boolean isDeprecated;

  private UserRoleInput userRole;

  private __EnumValueInput __enumValue;

  private __TypePossibleTypesInput __typePossibleTypes;

  private Integer version;

  private __FieldInput __field;

  private __InputValueInput __inputValue;

  public UserPhonesInput getUserPhones() {
    return this.userPhones;
  }

  public void setUserPhones(UserPhonesInput userPhones) {
    this.userPhones = userPhones;
  }

  public __TypeInterfacesInput get__typeInterfaces() {
    return this.__typeInterfaces;
  }

  public void set__typeInterfaces(__TypeInterfacesInput __typeInterfaces) {
    this.__typeInterfaces = __typeInterfaces;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __DirectiveLocationsInput get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(__DirectiveLocationsInput __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }

  public __TypeInput get__type() {
    return this.__type;
  }

  public void set__type(__TypeInput __type) {
    this.__type = __type;
  }

  public __DirectiveInput get__directive() {
    return this.__directive;
  }

  public void set__directive(__DirectiveInput __directive) {
    this.__directive = __directive;
  }

  public OrganizationInput getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationInput organization) {
    this.organization = organization;
  }

  public __SchemaInput get__schema() {
    return this.__schema;
  }

  public void set__schema(__SchemaInput __schema) {
    this.__schema = __schema;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public UserInput getUser() {
    return this.user;
  }

  public void setUser(UserInput user) {
    this.user = user;
  }

  public RoleInput getRole() {
    return this.role;
  }

  public void setRole(RoleInput role) {
    this.role = role;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public UserRoleInput getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRoleInput userRole) {
    this.userRole = userRole;
  }

  public __EnumValueInput get__enumValue() {
    return this.__enumValue;
  }

  public void set__enumValue(__EnumValueInput __enumValue) {
    this.__enumValue = __enumValue;
  }

  public __TypePossibleTypesInput get__typePossibleTypes() {
    return this.__typePossibleTypes;
  }

  public void set__typePossibleTypes(__TypePossibleTypesInput __typePossibleTypes) {
    this.__typePossibleTypes = __typePossibleTypes;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public __FieldInput get__field() {
    return this.__field;
  }

  public void set__field(__FieldInput __field) {
    this.__field = __field;
  }

  public __InputValueInput get__inputValue() {
    return this.__inputValue;
  }

  public void set__inputValue(__InputValueInput __inputValue) {
    this.__inputValue = __inputValue;
  }
}
