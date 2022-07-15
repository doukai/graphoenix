package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Skip;
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
@Skip
public class __Field implements Meta {
  @Id
  private String id;

  private String name;

  private String typeName;

  private String ofTypeName;

  private __Type ofType;

  private String description;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private Collection<__InputValue> args;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private __Type type;

  private String deprecationReason;

  private String from;

  private String to;

  private String withType;

  private String withFrom;

  private String withTo;

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

  private Integer nameCount;

  private String nameMax;

  private String nameMin;

  private Integer typeNameCount;

  private String typeNameMax;

  private String typeNameMin;

  private Integer ofTypeNameCount;

  private String ofTypeNameMax;

  private String ofTypeNameMin;

  private Integer descriptionCount;

  private String descriptionMax;

  private String descriptionMin;

  private Integer deprecationReasonCount;

  private String deprecationReasonMax;

  private String deprecationReasonMin;

  private Integer fromCount;

  private String fromMax;

  private String fromMin;

  private Integer toCount;

  private String toMax;

  private String toMin;

  private Integer withTypeCount;

  private String withTypeMax;

  private String withTypeMin;

  private Integer withFromCount;

  private String withFromMax;

  private String withFromMin;

  private Integer withToCount;

  private String withToMax;

  private String withToMin;

  private __InputValue argsAggregate;

  private __InputValueConnection argsConnection;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __Type getOfType() {
    return this.ofType;
  }

  public void setOfType(__Type ofType) {
    this.ofType = ofType;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<__InputValue> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValue> args) {
    this.args = args;
  }

  public __Type getType() {
    return this.type;
  }

  public void setType(__Type type) {
    this.type = type;
  }

  public String getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(String deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public String getFrom() {
    return this.from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return this.to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getWithType() {
    return this.withType;
  }

  public void setWithType(String withType) {
    this.withType = withType;
  }

  public String getWithFrom() {
    return this.withFrom;
  }

  public void setWithFrom(String withFrom) {
    this.withFrom = withFrom;
  }

  public String getWithTo() {
    return this.withTo;
  }

  public void setWithTo(String withTo) {
    this.withTo = withTo;
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

  public Integer getTypeNameCount() {
    return this.typeNameCount;
  }

  public void setTypeNameCount(Integer typeNameCount) {
    this.typeNameCount = typeNameCount;
  }

  public String getTypeNameMax() {
    return this.typeNameMax;
  }

  public void setTypeNameMax(String typeNameMax) {
    this.typeNameMax = typeNameMax;
  }

  public String getTypeNameMin() {
    return this.typeNameMin;
  }

  public void setTypeNameMin(String typeNameMin) {
    this.typeNameMin = typeNameMin;
  }

  public Integer getOfTypeNameCount() {
    return this.ofTypeNameCount;
  }

  public void setOfTypeNameCount(Integer ofTypeNameCount) {
    this.ofTypeNameCount = ofTypeNameCount;
  }

  public String getOfTypeNameMax() {
    return this.ofTypeNameMax;
  }

  public void setOfTypeNameMax(String ofTypeNameMax) {
    this.ofTypeNameMax = ofTypeNameMax;
  }

  public String getOfTypeNameMin() {
    return this.ofTypeNameMin;
  }

  public void setOfTypeNameMin(String ofTypeNameMin) {
    this.ofTypeNameMin = ofTypeNameMin;
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

  public Integer getDeprecationReasonCount() {
    return this.deprecationReasonCount;
  }

  public void setDeprecationReasonCount(Integer deprecationReasonCount) {
    this.deprecationReasonCount = deprecationReasonCount;
  }

  public String getDeprecationReasonMax() {
    return this.deprecationReasonMax;
  }

  public void setDeprecationReasonMax(String deprecationReasonMax) {
    this.deprecationReasonMax = deprecationReasonMax;
  }

  public String getDeprecationReasonMin() {
    return this.deprecationReasonMin;
  }

  public void setDeprecationReasonMin(String deprecationReasonMin) {
    this.deprecationReasonMin = deprecationReasonMin;
  }

  public Integer getFromCount() {
    return this.fromCount;
  }

  public void setFromCount(Integer fromCount) {
    this.fromCount = fromCount;
  }

  public String getFromMax() {
    return this.fromMax;
  }

  public void setFromMax(String fromMax) {
    this.fromMax = fromMax;
  }

  public String getFromMin() {
    return this.fromMin;
  }

  public void setFromMin(String fromMin) {
    this.fromMin = fromMin;
  }

  public Integer getToCount() {
    return this.toCount;
  }

  public void setToCount(Integer toCount) {
    this.toCount = toCount;
  }

  public String getToMax() {
    return this.toMax;
  }

  public void setToMax(String toMax) {
    this.toMax = toMax;
  }

  public String getToMin() {
    return this.toMin;
  }

  public void setToMin(String toMin) {
    this.toMin = toMin;
  }

  public Integer getWithTypeCount() {
    return this.withTypeCount;
  }

  public void setWithTypeCount(Integer withTypeCount) {
    this.withTypeCount = withTypeCount;
  }

  public String getWithTypeMax() {
    return this.withTypeMax;
  }

  public void setWithTypeMax(String withTypeMax) {
    this.withTypeMax = withTypeMax;
  }

  public String getWithTypeMin() {
    return this.withTypeMin;
  }

  public void setWithTypeMin(String withTypeMin) {
    this.withTypeMin = withTypeMin;
  }

  public Integer getWithFromCount() {
    return this.withFromCount;
  }

  public void setWithFromCount(Integer withFromCount) {
    this.withFromCount = withFromCount;
  }

  public String getWithFromMax() {
    return this.withFromMax;
  }

  public void setWithFromMax(String withFromMax) {
    this.withFromMax = withFromMax;
  }

  public String getWithFromMin() {
    return this.withFromMin;
  }

  public void setWithFromMin(String withFromMin) {
    this.withFromMin = withFromMin;
  }

  public Integer getWithToCount() {
    return this.withToCount;
  }

  public void setWithToCount(Integer withToCount) {
    this.withToCount = withToCount;
  }

  public String getWithToMax() {
    return this.withToMax;
  }

  public void setWithToMax(String withToMax) {
    this.withToMax = withToMax;
  }

  public String getWithToMin() {
    return this.withToMin;
  }

  public void setWithToMin(String withToMin) {
    this.withToMin = withToMin;
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
