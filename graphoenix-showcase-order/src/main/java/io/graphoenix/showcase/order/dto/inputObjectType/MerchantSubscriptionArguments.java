package io.graphoenix.showcase.order.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Conditional;
import io.graphoenix.core.dto.inputObjectType.IntExpression;
import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.showcase.user.dto.inputObjectType.OrganizationExpression;
import io.graphoenix.showcase.user.dto.inputObjectType.UserExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class MerchantSubscriptionArguments implements MetaExpression {
  private StringExpression id;

  private StringExpression name;

  private OrganizationExpression organization;

  private UserExpression customerServices;

  private OrganizationExpression partners;

  private UserExpression director;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression version;

  private IntExpression realmId;

  private StringExpression createUserId;

  private StringExpression createTime;

  private StringExpression updateUserId;

  private StringExpression updateTime;

  private StringExpression createGroupId;

  private StringExpression __typename;

  private IntExpression organizationId;

  private MerchantPartnersExpression merchantPartners;

  private MerchantDirectorExpression merchantDirector;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<MerchantExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
    this.id = id;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public OrganizationExpression getOrganization() {
    return this.organization;
  }

  public void setOrganization(OrganizationExpression organization) {
    this.organization = organization;
  }

  public UserExpression getCustomerServices() {
    return this.customerServices;
  }

  public void setCustomerServices(UserExpression customerServices) {
    this.customerServices = customerServices;
  }

  public OrganizationExpression getPartners() {
    return this.partners;
  }

  public void setPartners(OrganizationExpression partners) {
    this.partners = partners;
  }

  public UserExpression getDirector() {
    return this.director;
  }

  public void setDirector(UserExpression director) {
    this.director = director;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public IntExpression getRealmId() {
    return this.realmId;
  }

  public void setRealmId(IntExpression realmId) {
    this.realmId = realmId;
  }

  public StringExpression getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(StringExpression createUserId) {
    this.createUserId = createUserId;
  }

  public StringExpression getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(StringExpression createTime) {
    this.createTime = createTime;
  }

  public StringExpression getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(StringExpression updateUserId) {
    this.updateUserId = updateUserId;
  }

  public StringExpression getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(StringExpression updateTime) {
    this.updateTime = updateTime;
  }

  public StringExpression getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(StringExpression createGroupId) {
    this.createGroupId = createGroupId;
  }

  public StringExpression get__typename() {
    return this.__typename;
  }

  public void set__typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public IntExpression getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(IntExpression organizationId) {
    this.organizationId = organizationId;
  }

  public MerchantPartnersExpression getMerchantPartners() {
    return this.merchantPartners;
  }

  public void setMerchantPartners(MerchantPartnersExpression merchantPartners) {
    this.merchantPartners = merchantPartners;
  }

  public MerchantDirectorExpression getMerchantDirector() {
    return this.merchantDirector;
  }

  public void setMerchantDirector(MerchantDirectorExpression merchantDirector) {
    this.merchantDirector = merchantDirector;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<MerchantExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<MerchantExpression> exs) {
    this.exs = exs;
  }
}
