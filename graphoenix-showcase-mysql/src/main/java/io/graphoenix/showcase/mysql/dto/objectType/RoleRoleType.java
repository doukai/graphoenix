package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.enumType.RoleType;
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
public class RoleRoleType implements Meta {
  @Id
  private String id;

  private Integer roleId;

  private RoleType type;

  private Integer version;

  private Boolean isDeprecated;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer roleIdCount;

  private Integer roleIdSum;

  private Integer roleIdAvg;

  private Integer roleIdMax;

  private Integer roleIdMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getRoleId() {
    return this.roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }

  public RoleType getType() {
    return this.type;
  }

  public void setType(RoleType type) {
    this.type = type;
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

  public Integer getRoleIdCount() {
    return this.roleIdCount;
  }

  public void setRoleIdCount(Integer roleIdCount) {
    this.roleIdCount = roleIdCount;
  }

  public Integer getRoleIdSum() {
    return this.roleIdSum;
  }

  public void setRoleIdSum(Integer roleIdSum) {
    this.roleIdSum = roleIdSum;
  }

  public Integer getRoleIdAvg() {
    return this.roleIdAvg;
  }

  public void setRoleIdAvg(Integer roleIdAvg) {
    this.roleIdAvg = roleIdAvg;
  }

  public Integer getRoleIdMax() {
    return this.roleIdMax;
  }

  public void setRoleIdMax(Integer roleIdMax) {
    this.roleIdMax = roleIdMax;
  }

  public Integer getRoleIdMin() {
    return this.roleIdMin;
  }

  public void setRoleIdMin(Integer roleIdMin) {
    this.roleIdMin = roleIdMin;
  }
}