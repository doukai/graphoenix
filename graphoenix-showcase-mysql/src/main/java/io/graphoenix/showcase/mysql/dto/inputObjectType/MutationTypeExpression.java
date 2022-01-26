package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class MutationTypeExpression {
  private __EnumValueExpression __enumValue;

  private __TypeInterfacesExpression __typeInterfaces;

  private __FieldExpression __field;

  private __SchemaExpression __schema;

  private Collection<MutationTypeExpression> exs;

  private RoleExpression role;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private __TypeExpression __type;

  private OrganizationExpression organization;

  private __DirectiveLocationsExpression __directiveLocations;

  private UserExpression user;

  private IntExpression version;

  private UserPhonesExpression userPhones;

  @DefaultValue("AND")
  private Conditional cond;

  private __TypePossibleTypesExpression __typePossibleTypes;

  private UserRoleExpression userRole;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private __InputValueExpression __inputValue;

  private __DirectiveExpression __directive;

  public __EnumValueExpression get__enumValue() {
    return this.__enumValue;
  }

  public void set__enumValue(__EnumValueExpression __enumValue) {
    this.__enumValue = __enumValue;
  }

  public __TypeInterfacesExpression get__typeInterfaces() {
    return this.__typeInterfaces;
  }

  public void set__typeInterfaces(__TypeInterfacesExpression __typeInterfaces) {
    this.__typeInterfaces = __typeInterfaces;
  }

  public __FieldExpression get__field() {
    return this.__field;
  }

  public void set__field(__FieldExpression __field) {
    this.__field = __field;
  }

  public __SchemaExpression get__schema() {
    return this.__schema;
  }

  public void set__schema(__SchemaExpression __schema) {
    this.__schema = __schema;
  }

  public Collection<MutationTypeExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<MutationTypeExpression> exs) {
    this.exs = exs;
  }

  public RoleExpression getRole() {
    return this.role;
  }

  public void setRole(RoleExpression role) {
    this.role = role;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public __TypeExpression get__type() {
    return this.__type;
  }

  public void set__type(__TypeExpression __type) {
    this.__type = __type;
  }

  public OrganizationExpression getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationExpression organization) {
    this.organization = organization;
  }

  public __DirectiveLocationsExpression get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(__DirectiveLocationsExpression __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }

  public UserExpression getUser() {
    return this.user;
  }

  public void setUser(UserExpression user) {
    this.user = user;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public UserPhonesExpression getUserPhones() {
    return this.userPhones;
  }

  public void setUserPhones(UserPhonesExpression userPhones) {
    this.userPhones = userPhones;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public __TypePossibleTypesExpression get__typePossibleTypes() {
    return this.__typePossibleTypes;
  }

  public void set__typePossibleTypes(__TypePossibleTypesExpression __typePossibleTypes) {
    this.__typePossibleTypes = __typePossibleTypes;
  }

  public UserRoleExpression getUserRole() {
    return this.userRole;
  }

  public void setUserRole(UserRoleExpression userRole) {
    this.userRole = userRole;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public __InputValueExpression get__inputValue() {
    return this.__inputValue;
  }

  public void set__inputValue(__InputValueExpression __inputValue) {
    this.__inputValue = __inputValue;
  }

  public __DirectiveExpression get__directive() {
    return this.__directive;
  }

  public void set__directive(__DirectiveExpression __directive) {
    this.__directive = __directive;
  }
}
