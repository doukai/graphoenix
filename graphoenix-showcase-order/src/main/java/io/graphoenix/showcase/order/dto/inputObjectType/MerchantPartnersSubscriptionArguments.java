package io.graphoenix.showcase.order.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Conditional;
import io.graphoenix.core.dto.inputObjectType.IntExpression;
import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.showcase.user.dto.inputObjectType.OrganizationExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class MerchantPartnersSubscriptionArguments implements MetaExpression {
  private StringExpression id;

  private IntExpression merchantId;

  private MerchantExpression merchantIdType;

  private IntExpression organizationId;

  private OrganizationExpression organizationIdType;

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

  private Collection<String> groupBy;

  @DefaultValue("false")
  private Boolean not;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<MerchantPartnersExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
    this.id = id;
  }

  public IntExpression getMerchantId() {
    return this.merchantId;
  }

  public void setMerchantId(IntExpression merchantId) {
    this.merchantId = merchantId;
  }

  public MerchantExpression getMerchantIdType() {
    return this.merchantIdType;
  }

  public void setMerchantIdType(MerchantExpression merchantIdType) {
    this.merchantIdType = merchantIdType;
  }

  public IntExpression getOrganizationId() {
    return this.organizationId;
  }

  public void setOrganizationId(IntExpression organizationId) {
    this.organizationId = organizationId;
  }

  public OrganizationExpression getOrganizationIdType() {
    return this.organizationIdType;
  }

  public void setOrganizationIdType(OrganizationExpression organizationIdType) {
    this.organizationIdType = organizationIdType;
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

  public Collection<String> getGroupBy() {
    return this.groupBy;
  }

  public void setGroupBy(Collection<String> groupBy) {
    this.groupBy = groupBy;
  }

  public Boolean getNot() {
    return this.not;
  }

  public void setNot(Boolean not) {
    this.not = not;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<MerchantPartnersExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<MerchantPartnersExpression> exs) {
    this.exs = exs;
  }
}
