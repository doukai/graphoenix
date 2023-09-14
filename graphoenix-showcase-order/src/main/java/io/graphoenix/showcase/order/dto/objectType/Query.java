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
import java.util.Collection;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Query implements Meta {
  private Boolean isDeprecated;

  private Integer version;

  private Integer realmId;

  private String createUserId;

  private LocalDateTime createTime;

  private String updateUserId;

  private LocalDateTime updateTime;

  private String createGroupId;

  private Order order;

  private Collection<Order> orderList;

  private OrderConnection orderConnection;

  private Good good;

  private Collection<Good> goodList;

  private GoodConnection goodConnection;

  private Product product;

  private Collection<Product> productList;

  private ProductConnection productConnection;

  private Merchant merchant;

  private Collection<Merchant> merchantList;

  private MerchantConnection merchantConnection;

  private MerchantPartners merchantPartners;

  private Collection<MerchantPartners> merchantPartnersList;

  private MerchantPartnersConnection merchantPartnersConnection;

  private MerchantDirector merchantDirector;

  private Collection<MerchantDirector> merchantDirectorList;

  private MerchantDirectorConnection merchantDirectorConnection;

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

  public Order getOrder() {
    return this.order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Collection<Order> getOrderList() {
    return this.orderList;
  }

  public void setOrderList(Collection<Order> orderList) {
    this.orderList = orderList;
  }

  public OrderConnection getOrderConnection() {
    return this.orderConnection;
  }

  public void setOrderConnection(OrderConnection orderConnection) {
    this.orderConnection = orderConnection;
  }

  public Good getGood() {
    return this.good;
  }

  public void setGood(Good good) {
    this.good = good;
  }

  public Collection<Good> getGoodList() {
    return this.goodList;
  }

  public void setGoodList(Collection<Good> goodList) {
    this.goodList = goodList;
  }

  public GoodConnection getGoodConnection() {
    return this.goodConnection;
  }

  public void setGoodConnection(GoodConnection goodConnection) {
    this.goodConnection = goodConnection;
  }

  public Product getProduct() {
    return this.product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Collection<Product> getProductList() {
    return this.productList;
  }

  public void setProductList(Collection<Product> productList) {
    this.productList = productList;
  }

  public ProductConnection getProductConnection() {
    return this.productConnection;
  }

  public void setProductConnection(ProductConnection productConnection) {
    this.productConnection = productConnection;
  }

  public Merchant getMerchant() {
    return this.merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public Collection<Merchant> getMerchantList() {
    return this.merchantList;
  }

  public void setMerchantList(Collection<Merchant> merchantList) {
    this.merchantList = merchantList;
  }

  public MerchantConnection getMerchantConnection() {
    return this.merchantConnection;
  }

  public void setMerchantConnection(MerchantConnection merchantConnection) {
    this.merchantConnection = merchantConnection;
  }

  public MerchantPartners getMerchantPartners() {
    return this.merchantPartners;
  }

  public void setMerchantPartners(MerchantPartners merchantPartners) {
    this.merchantPartners = merchantPartners;
  }

  public Collection<MerchantPartners> getMerchantPartnersList() {
    return this.merchantPartnersList;
  }

  public void setMerchantPartnersList(Collection<MerchantPartners> merchantPartnersList) {
    this.merchantPartnersList = merchantPartnersList;
  }

  public MerchantPartnersConnection getMerchantPartnersConnection() {
    return this.merchantPartnersConnection;
  }

  public void setMerchantPartnersConnection(MerchantPartnersConnection merchantPartnersConnection) {
    this.merchantPartnersConnection = merchantPartnersConnection;
  }

  public MerchantDirector getMerchantDirector() {
    return this.merchantDirector;
  }

  public void setMerchantDirector(MerchantDirector merchantDirector) {
    this.merchantDirector = merchantDirector;
  }

  public Collection<MerchantDirector> getMerchantDirectorList() {
    return this.merchantDirectorList;
  }

  public void setMerchantDirectorList(Collection<MerchantDirector> merchantDirectorList) {
    this.merchantDirectorList = merchantDirectorList;
  }

  public MerchantDirectorConnection getMerchantDirectorConnection() {
    return this.merchantDirectorConnection;
  }

  public void setMerchantDirectorConnection(MerchantDirectorConnection merchantDirectorConnection) {
    this.merchantDirectorConnection = merchantDirectorConnection;
  }
}
