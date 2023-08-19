package io.graphoenix.introspection.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.__DirectiveLocation;
import io.graphoenix.core.dto.interfaceType.Meta;
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
  @NonNull
  private String name;

  private __Schema ofSchema;

  private String description;

  @NonNull
  private Collection<__DirectiveLocation> locations;

  @NonNull
  private Collection<__InputValue> args;

  @NonNull
  private Boolean isRepeatable;

  private Boolean isDeprecated;

  private Integer version;

  private Integer realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private String __typename;

  private Integer nameCount;

  private String nameMax;

  private String nameMin;

  private Integer descriptionCount;

  private String descriptionMax;

  private String descriptionMin;

  private __InputValue argsAggregate;

  private __InputValueConnection argsConnection;

  private Integer schemaId;

  private Integer schemaIdCount;

  private Integer schemaIdSum;

  private Integer schemaIdAvg;

  private Integer schemaIdMax;

  private Integer schemaIdMin;

  private Collection<__DirectiveLocations> __directiveLocations;

  private __DirectiveLocations __directiveLocationsAggregate;

  private __DirectiveLocationsConnection __directiveLocationsConnection;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public __Schema getOfSchema() {
    return this.ofSchema;
  }

  public void setOfSchema(__Schema ofSchema) {
    this.ofSchema = ofSchema;
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

  public Boolean getIsRepeatable() {
    return this.isRepeatable;
  }

  public void setIsRepeatable(Boolean isRepeatable) {
    this.isRepeatable = isRepeatable;
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
  public Integer getRealmId() {
    return this.realmId;
  }

  @Override
  public void setRealmId(Integer realmId) {
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

  public Integer getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(Integer schemaId) {
    this.schemaId = schemaId;
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

  public Collection<__DirectiveLocations> get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(Collection<__DirectiveLocations> __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }

  public __DirectiveLocations get__directiveLocationsAggregate() {
    return this.__directiveLocationsAggregate;
  }

  public void set__directiveLocationsAggregate(__DirectiveLocations __directiveLocationsAggregate) {
    this.__directiveLocationsAggregate = __directiveLocationsAggregate;
  }

  public __DirectiveLocationsConnection get__directiveLocationsConnection() {
    return this.__directiveLocationsConnection;
  }

  public void set__directiveLocationsConnection(
      __DirectiveLocationsConnection __directiveLocationsConnection) {
    this.__directiveLocationsConnection = __directiveLocationsConnection;
  }
}
