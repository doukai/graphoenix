package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import null.dto.objectType.__Type;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __InputValue implements Meta {
  @Id
  private String id;

  private String name;

  private String typeName;

  private String ofTypeName;

  private __Type ofType;

  private Integer fieldId;

  private String directiveName;

  private String description;

  @NonNull
  private __Type type;

  private String defaultValue;

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

  private Integer directiveNameCount;

  private String directiveNameMax;

  private String directiveNameMin;

  private Integer descriptionCount;

  private String descriptionMax;

  private String descriptionMin;

  private Integer defaultValueCount;

  private String defaultValueMax;

  private String defaultValueMin;

  private Integer fieldIdCount;

  private Integer fieldIdSum;

  private Integer fieldIdAvg;

  private Integer fieldIdMax;

  private Integer fieldIdMin;

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

  public Integer getFieldId() {
    return this.fieldId;
  }

  public void setFieldId(Integer fieldId) {
    this.fieldId = fieldId;
  }

  public String getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(String directiveName) {
    this.directiveName = directiveName;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public __Type getType() {
    return this.type;
  }

  public void setType(__Type type) {
    this.type = type;
  }

  public String getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
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

  public Integer getDirectiveNameCount() {
    return this.directiveNameCount;
  }

  public void setDirectiveNameCount(Integer directiveNameCount) {
    this.directiveNameCount = directiveNameCount;
  }

  public String getDirectiveNameMax() {
    return this.directiveNameMax;
  }

  public void setDirectiveNameMax(String directiveNameMax) {
    this.directiveNameMax = directiveNameMax;
  }

  public String getDirectiveNameMin() {
    return this.directiveNameMin;
  }

  public void setDirectiveNameMin(String directiveNameMin) {
    this.directiveNameMin = directiveNameMin;
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

  public Integer getDefaultValueCount() {
    return this.defaultValueCount;
  }

  public void setDefaultValueCount(Integer defaultValueCount) {
    this.defaultValueCount = defaultValueCount;
  }

  public String getDefaultValueMax() {
    return this.defaultValueMax;
  }

  public void setDefaultValueMax(String defaultValueMax) {
    this.defaultValueMax = defaultValueMax;
  }

  public String getDefaultValueMin() {
    return this.defaultValueMin;
  }

  public void setDefaultValueMin(String defaultValueMin) {
    this.defaultValueMin = defaultValueMin;
  }

  public Integer getFieldIdCount() {
    return this.fieldIdCount;
  }

  public void setFieldIdCount(Integer fieldIdCount) {
    this.fieldIdCount = fieldIdCount;
  }

  public Integer getFieldIdSum() {
    return this.fieldIdSum;
  }

  public void setFieldIdSum(Integer fieldIdSum) {
    this.fieldIdSum = fieldIdSum;
  }

  public Integer getFieldIdAvg() {
    return this.fieldIdAvg;
  }

  public void setFieldIdAvg(Integer fieldIdAvg) {
    this.fieldIdAvg = fieldIdAvg;
  }

  public Integer getFieldIdMax() {
    return this.fieldIdMax;
  }

  public void setFieldIdMax(Integer fieldIdMax) {
    this.fieldIdMax = fieldIdMax;
  }

  public Integer getFieldIdMin() {
    return this.fieldIdMin;
  }

  public void setFieldIdMin(Integer fieldIdMin) {
    this.fieldIdMin = fieldIdMin;
  }
}
