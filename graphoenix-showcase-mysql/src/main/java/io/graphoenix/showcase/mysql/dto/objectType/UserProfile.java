package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class UserProfile implements Meta {
  @Id
  private String id;

  private String userId;

  private String email;

  private String address;

  private String qq;

  private User user;

  private User rpcUser;

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

  private Integer userIdCount;

  private String userIdMax;

  private String userIdMin;

  private Integer emailCount;

  private String emailMax;

  private String emailMin;

  private Integer addressCount;

  private String addressMax;

  private String addressMin;

  private Integer qqCount;

  private String qqMax;

  private String qqMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return this.userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return this.address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getQq() {
    return this.qq;
  }

  public void setQq(String qq) {
    this.qq = qq;
  }

  public User getUser() {
    return this.user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public User getRpcUser() {
    return this.rpcUser;
  }

  public void setRpcUser(User rpcUser) {
    this.rpcUser = rpcUser;
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

  public Integer getUserIdCount() {
    return this.userIdCount;
  }

  public void setUserIdCount(Integer userIdCount) {
    this.userIdCount = userIdCount;
  }

  public String getUserIdMax() {
    return this.userIdMax;
  }

  public void setUserIdMax(String userIdMax) {
    this.userIdMax = userIdMax;
  }

  public String getUserIdMin() {
    return this.userIdMin;
  }

  public void setUserIdMin(String userIdMin) {
    this.userIdMin = userIdMin;
  }

  public Integer getEmailCount() {
    return this.emailCount;
  }

  public void setEmailCount(Integer emailCount) {
    this.emailCount = emailCount;
  }

  public String getEmailMax() {
    return this.emailMax;
  }

  public void setEmailMax(String emailMax) {
    this.emailMax = emailMax;
  }

  public String getEmailMin() {
    return this.emailMin;
  }

  public void setEmailMin(String emailMin) {
    this.emailMin = emailMin;
  }

  public Integer getAddressCount() {
    return this.addressCount;
  }

  public void setAddressCount(Integer addressCount) {
    this.addressCount = addressCount;
  }

  public String getAddressMax() {
    return this.addressMax;
  }

  public void setAddressMax(String addressMax) {
    this.addressMax = addressMax;
  }

  public String getAddressMin() {
    return this.addressMin;
  }

  public void setAddressMin(String addressMin) {
    this.addressMin = addressMin;
  }

  public Integer getQqCount() {
    return this.qqCount;
  }

  public void setQqCount(Integer qqCount) {
    this.qqCount = qqCount;
  }

  public String getQqMax() {
    return this.qqMax;
  }

  public void setQqMax(String qqMax) {
    this.qqMax = qqMax;
  }

  public String getQqMin() {
    return this.qqMin;
  }

  public void setQqMin(String qqMin) {
    this.qqMin = qqMin;
  }
}
