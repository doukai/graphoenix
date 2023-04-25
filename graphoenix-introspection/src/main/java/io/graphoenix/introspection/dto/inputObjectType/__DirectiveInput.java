package io.graphoenix.introspection.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.__DirectiveLocation;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __DirectiveInput {
  private String name;

  private Integer schemaId;

  private String description;

  private Collection<__DirectiveLocation> locations;

  private Collection<__InputValueInput> args;

  private Boolean isRepeatable;

  private Boolean isDeprecated;

  private Integer version;

  private String realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  @DefaultValue("\"__Directive\"")
  private String __typename;

  private Collection<__DirectiveLocationsInput> __directiveLocations;

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

  public Collection<__InputValueInput> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValueInput> args) {
    this.args = args;
  }

  public Boolean getIsRepeatable() {
    return this.isRepeatable;
  }

  public void setIsRepeatable(Boolean isRepeatable) {
    this.isRepeatable = isRepeatable;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getRealmId() {
    return this.realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public String getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public String getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(String updateUserId) {
    this.updateUserId = updateUserId;
  }

  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public String getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(String createGroupId) {
    this.createGroupId = createGroupId;
  }

  public String get__typename() {
    return this.__typename;
  }

  public void set__typename(String __typename) {
    this.__typename = __typename;
  }

  public Collection<__DirectiveLocationsInput> get__directiveLocations() {
    return this.__directiveLocations;
  }

  public void set__directiveLocations(Collection<__DirectiveLocationsInput> __directiveLocations) {
    this.__directiveLocations = __directiveLocations;
  }
}
