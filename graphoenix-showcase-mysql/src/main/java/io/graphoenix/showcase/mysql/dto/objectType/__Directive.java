package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Ignore;
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
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __Directive implements Meta {
  @Id
  private String name;

  private Integer schemaId;

  private String description;

  @NonNull
  private Collection<__DirectiveLocation> locations;

  @NonNull
  private Collection<__InputValue> args;

  private Boolean onOperation;

  private Boolean onFragment;

  private Boolean onField;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String __typename;

  private Integer nameCount;

  @Id
  private String nameMax;

  @Id
  private String nameMin;

  private Integer descriptionCount;

  private String descriptionMax;

  private String descriptionMin;

  private Integer schemaIdCount;

  private Integer schemaIdSum;

  private Integer schemaIdAvg;

  private Integer schemaIdMax;

  private Integer schemaIdMin;

  private __InputValue argsAggregate;

  private __InputValueConnection argsConnection;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Integer schemaId) {
    this.schemaId = schemaId;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<__DirectiveLocation> getLocations() {
    return this.locations;
  }

  public void setLocations(Collection<__DirectiveLocation> locations) {
    this.locations = locations;
  }

  public Collection<__InputValue> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValue> args) {
    this.args = args;
  }

  public Boolean getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(Boolean onOperation) {
    this.onOperation = onOperation;
  }

  public Boolean getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(Boolean onFragment) {
    this.onFragment = onFragment;
  }

  public Boolean getOnField() {
    return this.onField;
  }

  public void setOnField(Boolean onField) {
    this.onField = onField;
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

  public Integer getDescriptionCount() {
    return this.descriptionCount;
  }

  public void setDescriptionCount(Integer descriptionCount) {
    this.descriptionCount = descriptionCount;
  }

  public String getDescriptionMax() {
    return this.descriptionMax;
  }

  public void setDescriptionMax(String descriptionMax) {
    this.descriptionMax = descriptionMax;
  }

  public String getDescriptionMin() {
    return this.descriptionMin;
  }

  public void setDescriptionMin(String descriptionMin) {
    this.descriptionMin = descriptionMin;
  }

  public Integer getSchemaIdCount() {
    return this.schemaIdCount;
  }

  public void setSchemaIdCount(Integer schemaIdCount) {
    this.schemaIdCount = schemaIdCount;
  }

  public Integer getSchemaIdSum() {
    return this.schemaIdSum;
  }

  public void setSchemaIdSum(Integer schemaIdSum) {
    this.schemaIdSum = schemaIdSum;
  }

  public Integer getSchemaIdAvg() {
    return this.schemaIdAvg;
  }

  public void setSchemaIdAvg(Integer schemaIdAvg) {
    this.schemaIdAvg = schemaIdAvg;
  }

  public Integer getSchemaIdMax() {
    return this.schemaIdMax;
  }

  public void setSchemaIdMax(Integer schemaIdMax) {
    this.schemaIdMax = schemaIdMax;
  }

  public Integer getSchemaIdMin() {
    return this.schemaIdMin;
  }

  public void setSchemaIdMin(Integer schemaIdMin) {
    this.schemaIdMin = schemaIdMin;
  }

  public __InputValue getArgsAggregate() {
    return this.argsAggregate;
  }

  public void setArgsAggregate(__InputValue argsAggregate) {
    this.argsAggregate = argsAggregate;
  }

  public __InputValueConnection getArgsConnection() {
    return this.argsConnection;
  }

  public void setArgsConnection(__InputValueConnection argsConnection) {
    this.argsConnection = argsConnection;
  }
}
