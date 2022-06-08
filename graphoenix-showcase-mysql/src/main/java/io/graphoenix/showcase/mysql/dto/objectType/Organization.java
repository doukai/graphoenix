package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class Organization implements Meta {
  @Id
  private String id;

  private Integer aboveId;

  private Organization above;

  private Collection<User> users;

  @NonNull
  private String name;

  private String domainId;

  private Boolean isDeprecated;

  private Integer version;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createOrganizationId;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer nameCount;

  private String nameMax;

  private String nameMin;

  private Integer aboveIdCount;

  private Integer aboveIdSum;

  private Integer aboveIdAvg;

  private Integer aboveIdMax;

  private Integer aboveIdMin;

  private User usersAggregate;

  private UserConnection usersConnection;

  private Integer orgLevel2;

  private Collection<Integer> orgLevel3;

  private Boolean roleDisable;

  private Collection<User> userByOrg;

  private Organization parent;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getAboveId() {
    return this.aboveId;
  }

  public void setAboveId(Integer aboveId) {
    this.aboveId = aboveId;
  }

  public Organization getAbove() {
    return this.above;
  }

  public void setAbove(Organization above) {
    this.above = above;
  }

  public Collection<User> getUsers() {
    return this.users;
  }

  public void setUsers(Collection<User> users) {
    this.users = users;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDomainId() {
    return this.domainId;
  }

  @Override
  public void setDomainId(String domainId) {
    this.domainId = domainId;
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
  public String getCreateOrganizationId() {
    return this.createOrganizationId;
  }

  @Override
  public void setCreateOrganizationId(String createOrganizationId) {
    this.createOrganizationId = createOrganizationId;
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

  public Integer getAboveIdCount() {
    return this.aboveIdCount;
  }

  public void setAboveIdCount(Integer aboveIdCount) {
    this.aboveIdCount = aboveIdCount;
  }

  public Integer getAboveIdSum() {
    return this.aboveIdSum;
  }

  public void setAboveIdSum(Integer aboveIdSum) {
    this.aboveIdSum = aboveIdSum;
  }

  public Integer getAboveIdAvg() {
    return this.aboveIdAvg;
  }

  public void setAboveIdAvg(Integer aboveIdAvg) {
    this.aboveIdAvg = aboveIdAvg;
  }

  public Integer getAboveIdMax() {
    return this.aboveIdMax;
  }

  public void setAboveIdMax(Integer aboveIdMax) {
    this.aboveIdMax = aboveIdMax;
  }

  public Integer getAboveIdMin() {
    return this.aboveIdMin;
  }

  public void setAboveIdMin(Integer aboveIdMin) {
    this.aboveIdMin = aboveIdMin;
  }

  public User getUsersAggregate() {
    return this.usersAggregate;
  }

  public void setUsersAggregate(User usersAggregate) {
    this.usersAggregate = usersAggregate;
  }

  public UserConnection getUsersConnection() {
    return this.usersConnection;
  }

  public void setUsersConnection(UserConnection usersConnection) {
    this.usersConnection = usersConnection;
  }

  public Integer getOrgLevel2() {
    return this.orgLevel2;
  }

  public void setOrgLevel2(Integer orgLevel2) {
    this.orgLevel2 = orgLevel2;
  }

  public Collection<Integer> getOrgLevel3() {
    return this.orgLevel3;
  }

  public void setOrgLevel3(Collection<Integer> orgLevel3) {
    this.orgLevel3 = orgLevel3;
  }

  public Boolean getRoleDisable() {
    return this.roleDisable;
  }

  public void setRoleDisable(Boolean roleDisable) {
    this.roleDisable = roleDisable;
  }

  public Collection<User> getUserByOrg() {
    return this.userByOrg;
  }

  public void setUserByOrg(Collection<User> userByOrg) {
    this.userByOrg = userByOrg;
  }

  public Organization getParent() {
    return this.parent;
  }

  public void setParent(Organization parent) {
    this.parent = parent;
  }
}
