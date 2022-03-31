package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class UserPhones implements Meta {
  @Id
  private String id;

  private Integer userId;

  private String phone;

  private Integer version;

  private Boolean isDeprecated;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer phoneCount;

  private String phoneMax;

  private String phoneMin;

  private Integer userIdCount;

  private Integer userIdSum;

  private Integer userIdAvg;

  private Integer userIdMax;

  private Integer userIdMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getPhone() {
    return this.phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
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

  public Integer getPhoneCount() {
    return this.phoneCount;
  }

  public void setPhoneCount(Integer phoneCount) {
    this.phoneCount = phoneCount;
  }

  public String getPhoneMax() {
    return this.phoneMax;
  }

  public void setPhoneMax(String phoneMax) {
    this.phoneMax = phoneMax;
  }

  public String getPhoneMin() {
    return this.phoneMin;
  }

  public void setPhoneMin(String phoneMin) {
    this.phoneMin = phoneMin;
  }

  public Integer getUserIdCount() {
    return this.userIdCount;
  }

  public void setUserIdCount(Integer userIdCount) {
    this.userIdCount = userIdCount;
  }

  public Integer getUserIdSum() {
    return this.userIdSum;
  }

  public void setUserIdSum(Integer userIdSum) {
    this.userIdSum = userIdSum;
  }

  public Integer getUserIdAvg() {
    return this.userIdAvg;
  }

  public void setUserIdAvg(Integer userIdAvg) {
    this.userIdAvg = userIdAvg;
  }

  public Integer getUserIdMax() {
    return this.userIdMax;
  }

  public void setUserIdMax(Integer userIdMax) {
    this.userIdMax = userIdMax;
  }

  public Integer getUserIdMin() {
    return this.userIdMin;
  }

  public void setUserIdMin(Integer userIdMin) {
    this.userIdMin = userIdMin;
  }
}
