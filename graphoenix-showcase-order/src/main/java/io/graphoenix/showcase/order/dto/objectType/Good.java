package io.graphoenix.showcase.order.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDateTime;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Good implements Meta {
  @Id
  private String id;

  @NonNull
  private Integer quantity;

  @NonNull
  private Order order;

  @NonNull
  private Product product;

  @NonNull
  private Merchant merchant;

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

  private Integer idMax;

  private Integer idMin;

  private Integer quantityCount;

  private Integer quantitySum;

  private Integer quantityAvg;

  private Integer quantityMax;

  private Integer quantityMin;

  private Integer orderId;

  private Integer orderIdCount;

  private Integer orderIdSum;

  private Integer orderIdAvg;

  private Integer orderIdMax;

  private Integer orderIdMin;

  private Integer productId;

  private Integer productIdCount;

  private Integer productIdSum;

  private Integer productIdAvg;

  private Integer productIdMax;

  private Integer productIdMin;

  private Integer merchantId;

  private Integer merchantIdCount;

  private Integer merchantIdSum;

  private Integer merchantIdAvg;

  private Integer merchantIdMax;

  private Integer merchantIdMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getQuantity() {
    return this.quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Order getOrder() {
    return this.order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Product getProduct() {
    return this.product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Merchant getMerchant() {
    return this.merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
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

  public Integer getIdMax() {
    return this.idMax;
  }

  public void setIdMax(Integer idMax) {
    this.idMax = idMax;
  }

  public Integer getIdMin() {
    return this.idMin;
  }

  public void setIdMin(Integer idMin) {
    this.idMin = idMin;
  }

  public Integer getQuantityCount() {
    return this.quantityCount;
  }

  public void setQuantityCount(Integer quantityCount) {
    this.quantityCount = quantityCount;
  }

  public Integer getQuantitySum() {
    return this.quantitySum;
  }

  public void setQuantitySum(Integer quantitySum) {
    this.quantitySum = quantitySum;
  }

  public Integer getQuantityAvg() {
    return this.quantityAvg;
  }

  public void setQuantityAvg(Integer quantityAvg) {
    this.quantityAvg = quantityAvg;
  }

  public Integer getQuantityMax() {
    return this.quantityMax;
  }

  public void setQuantityMax(Integer quantityMax) {
    this.quantityMax = quantityMax;
  }

  public Integer getQuantityMin() {
    return this.quantityMin;
  }

  public void setQuantityMin(Integer quantityMin) {
    this.quantityMin = quantityMin;
  }

  public Integer getOrderId() {
    return this.orderId;
  }

  public void setOrderId(Integer orderId) {
    this.orderId = orderId;
  }

  public Integer getOrderIdCount() {
    return this.orderIdCount;
  }

  public void setOrderIdCount(Integer orderIdCount) {
    this.orderIdCount = orderIdCount;
  }

  public Integer getOrderIdSum() {
    return this.orderIdSum;
  }

  public void setOrderIdSum(Integer orderIdSum) {
    this.orderIdSum = orderIdSum;
  }

  public Integer getOrderIdAvg() {
    return this.orderIdAvg;
  }

  public void setOrderIdAvg(Integer orderIdAvg) {
    this.orderIdAvg = orderIdAvg;
  }

  public Integer getOrderIdMax() {
    return this.orderIdMax;
  }

  public void setOrderIdMax(Integer orderIdMax) {
    this.orderIdMax = orderIdMax;
  }

  public Integer getOrderIdMin() {
    return this.orderIdMin;
  }

  public void setOrderIdMin(Integer orderIdMin) {
    this.orderIdMin = orderIdMin;
  }

  public Integer getProductId() {
    return this.productId;
  }

  public void setProductId(Integer productId) {
    this.productId = productId;
  }

  public Integer getProductIdCount() {
    return this.productIdCount;
  }

  public void setProductIdCount(Integer productIdCount) {
    this.productIdCount = productIdCount;
  }

  public Integer getProductIdSum() {
    return this.productIdSum;
  }

  public void setProductIdSum(Integer productIdSum) {
    this.productIdSum = productIdSum;
  }

  public Integer getProductIdAvg() {
    return this.productIdAvg;
  }

  public void setProductIdAvg(Integer productIdAvg) {
    this.productIdAvg = productIdAvg;
  }

  public Integer getProductIdMax() {
    return this.productIdMax;
  }

  public void setProductIdMax(Integer productIdMax) {
    this.productIdMax = productIdMax;
  }

  public Integer getProductIdMin() {
    return this.productIdMin;
  }

  public void setProductIdMin(Integer productIdMin) {
    this.productIdMin = productIdMin;
  }

  public Integer getMerchantId() {
    return this.merchantId;
  }

  public void setMerchantId(Integer merchantId) {
    this.merchantId = merchantId;
  }

  public Integer getMerchantIdCount() {
    return this.merchantIdCount;
  }

  public void setMerchantIdCount(Integer merchantIdCount) {
    this.merchantIdCount = merchantIdCount;
  }

  public Integer getMerchantIdSum() {
    return this.merchantIdSum;
  }

  public void setMerchantIdSum(Integer merchantIdSum) {
    this.merchantIdSum = merchantIdSum;
  }

  public Integer getMerchantIdAvg() {
    return this.merchantIdAvg;
  }

  public void setMerchantIdAvg(Integer merchantIdAvg) {
    this.merchantIdAvg = merchantIdAvg;
  }

  public Integer getMerchantIdMax() {
    return this.merchantIdMax;
  }

  public void setMerchantIdMax(Integer merchantIdMax) {
    this.merchantIdMax = merchantIdMax;
  }

  public Integer getMerchantIdMin() {
    return this.merchantIdMin;
  }

  public void setMerchantIdMin(Integer merchantIdMin) {
    this.merchantIdMin = merchantIdMin;
  }
}
