package io.graphoenix.showcase.user.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.user.dto.enumType.Sort;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class UserMobileNumbersOrderBy {
  private Sort id;

  private Sort userId;

  private Sort mobileNumber;

  private Sort isDeprecated;

  private Sort version;

  private Sort realmId;

  private Sort createUserId;

  private Sort createTime;

  private Sort updateUserId;

  private Sort updateTime;

  private Sort createGroupId;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getUserId() {
    return this.userId;
  }

  public void setUserId(Sort userId) {
    this.userId = userId;
  }

  public Sort getMobileNumber() {
    return this.mobileNumber;
  }

  public void setMobileNumber(Sort mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public Sort getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Sort isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Sort getVersion() {
    return this.version;
  }

  public void setVersion(Sort version) {
    this.version = version;
  }

  public Sort getRealmId() {
    return this.realmId;
  }

  public void setRealmId(Sort realmId) {
    this.realmId = realmId;
  }

  public Sort getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(Sort createUserId) {
    this.createUserId = createUserId;
  }

  public Sort getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(Sort createTime) {
    this.createTime = createTime;
  }

  public Sort getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(Sort updateUserId) {
    this.updateUserId = updateUserId;
  }

  public Sort getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(Sort updateTime) {
    this.updateTime = updateTime;
  }

  public Sort getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(Sort createGroupId) {
    this.createGroupId = createGroupId;
  }

  public Sort get__typename() {
    return this.__typename;
  }

  public void set__typename(Sort __typename) {
    this.__typename = __typename;
  }
}
