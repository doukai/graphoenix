package io.graphoenix.showcase.order.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.user.dto.inputObjectType.OrganizationInput;
import io.graphoenix.showcase.user.dto.inputObjectType.UserInput;
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
public class MerchantListMutationTypeArguments {
  private String id;

  private String name;

  private OrganizationInput organization;

  private Collection<UserInput> customerServices;

  private Collection<OrganizationInput> partners;

  private UserInput director;

  private Boolean isDeprecated;

  private Integer version;

  private Integer realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  @DefaultValue("\"Merchant\"")
  private String __typename;

  private Integer organizationId;

  private Collection<MerchantPartnersInput> merchantPartners;

  private MerchantDirectorInput merchantDirector;

  private Collection<MerchantInput> list;

  private MerchantExpression where;

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

  public OrganizationInput getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationInput organization) {
    this.organization = organization;
  }

  public Collection<UserInput> getCustomerServices() {
    return this.customerServices;
  }

  public void setCustomerServices(Collection<UserInput> customerServices) {
    this.customerServices = customerServices;
  }

  public Collection<OrganizationInput> getPartners() {
    return this.partners;
  }

  public void setPartners(Collection<OrganizationInput> partners) {
    this.partners = partners;
  }

  public UserInput getDirector() {
    return this.director;
  }

  public void setDirector(UserInput director) {
    this.director = director;
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

  public Integer getRealmId() {
    return this.realmId;
  }

  public void setRealmId(Integer realmId) {
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

  public Integer getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(Integer organizationId) {
    this.organizationId = organizationId;
  }

  public Collection<MerchantPartnersInput> getMerchantPartners() {
    return this.merchantPartners;
  }

  public void setMerchantPartners(Collection<MerchantPartnersInput> merchantPartners) {
    this.merchantPartners = merchantPartners;
  }

  public MerchantDirectorInput getMerchantDirector() {
    return this.merchantDirector;
  }

  public void setMerchantDirector(MerchantDirectorInput merchantDirector) {
    this.merchantDirector = merchantDirector;
  }

  public Collection<MerchantInput> getList() {
    return this.list;
  }

  public void setList(Collection<MerchantInput> list) {
    this.list = list;
  }

  public MerchantExpression getWhere() {
    return this.where;
  }

  public void setWhere(MerchantExpression where) {
    this.where = where;
  }
}
