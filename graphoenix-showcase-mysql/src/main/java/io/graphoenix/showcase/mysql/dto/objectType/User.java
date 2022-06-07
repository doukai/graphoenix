package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
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

  private Integer organizationId;

  private Organization organization;

  private Collection<Role> roles;

  @NonNull
  private Collection<String> phones;

  @NonNull
  private Collection<Integer> test1;

  @NonNull
  private Collection<Boolean> test2;

  private Integer version;

  private Boolean isDeprecated;

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

  private Integer phonesCount;

  private String phonesMax;

  private String phonesMin;

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

  private Integer test1Count;

  private Integer test1Sum;

  private Integer test1Avg;

  private Integer test1Max;

  private Integer test1Min;

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

  public Integer getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(Integer organizationId) {
    this.organizationId = organizationId;
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
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
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

  public Integer getPhonesCount() {
    return this.phonesCount;
  }

  public void setPhonesCount(Integer phonesCount) {
    this.phonesCount = phonesCount;
  }

  public String getPhonesMax() {
    return this.phonesMax;
  }

  public void setPhonesMax(String phonesMax) {
    this.phonesMax = phonesMax;
  }

  public String getPhonesMin() {
    return this.phonesMin;
  }

  public void setPhonesMin(String phonesMin) {
    this.phonesMin = phonesMin;
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

  public Integer getTest1Count() {
    return this.test1Count;
  }

  public void setTest1Count(Integer test1Count) {
    this.test1Count = test1Count;
  }

  public Integer getTest1Sum() {
    return this.test1Sum;
  }

  public void setTest1Sum(Integer test1Sum) {
    this.test1Sum = test1Sum;
  }

  public Integer getTest1Avg() {
    return this.test1Avg;
  }

  public void setTest1Avg(Integer test1Avg) {
    this.test1Avg = test1Avg;
  }

  public Integer getTest1Max() {
    return this.test1Max;
  }

  public void setTest1Max(Integer test1Max) {
    this.test1Max = test1Max;
  }

  public Integer getTest1Min() {
    return this.test1Min;
  }

  public void setTest1Min(Integer test1Min) {
    this.test1Min = test1Min;
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
