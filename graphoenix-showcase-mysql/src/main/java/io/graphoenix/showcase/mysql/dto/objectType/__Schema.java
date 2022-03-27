package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __Schema implements Meta {
  @Id
  private String id;

  private String queryTypeName;

  private String mutationTypeName;

  private String subscriptionTypeName;

  @NonNull
  private Collection<__Type> types;

  @NonNull
  private __Type queryType;

  private __Type mutationType;

  private __Type subscriptionType;

  @NonNull
  private Collection<__Directive> directives;

  private Integer version;

  private Boolean isDeprecated;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer queryTypeNameCount;

  private String queryTypeNameMax;

  private String queryTypeNameMin;

  private Integer mutationTypeNameCount;

  private String mutationTypeNameMax;

  private String mutationTypeNameMin;

  private Integer subscriptionTypeNameCount;

  private String subscriptionTypeNameMax;

  private String subscriptionTypeNameMin;

  private __Type typesAggregate;

  private __Directive directivesAggregate;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(String queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public String getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(String mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public String getSubscriptionTypeName() {
    return this.subscriptionTypeName;
  }

  public void setSubscriptionTypeName(String subscriptionTypeName) {
    this.subscriptionTypeName = subscriptionTypeName;
  }

  public Collection<__Type> getTypes() {
    return this.types;
  }

  public void setTypes(Collection<__Type> types) {
    this.types = types;
  }

  public __Type getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__Type queryType) {
    this.queryType = queryType;
  }

  public __Type getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__Type mutationType) {
    this.mutationType = mutationType;
  }

  public __Type getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__Type subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public Collection<__Directive> getDirectives() {
    return this.directives;
  }

  public void setDirectives(Collection<__Directive> directives) {
    this.directives = directives;
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
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
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

  public Integer getQueryTypeNameCount() {
    return this.queryTypeNameCount;
  }

  public void setQueryTypeNameCount(Integer queryTypeNameCount) {
    this.queryTypeNameCount = queryTypeNameCount;
  }

  public String getQueryTypeNameMax() {
    return this.queryTypeNameMax;
  }

  public void setQueryTypeNameMax(String queryTypeNameMax) {
    this.queryTypeNameMax = queryTypeNameMax;
  }

  public String getQueryTypeNameMin() {
    return this.queryTypeNameMin;
  }

  public void setQueryTypeNameMin(String queryTypeNameMin) {
    this.queryTypeNameMin = queryTypeNameMin;
  }

  public Integer getMutationTypeNameCount() {
    return this.mutationTypeNameCount;
  }

  public void setMutationTypeNameCount(Integer mutationTypeNameCount) {
    this.mutationTypeNameCount = mutationTypeNameCount;
  }

  public String getMutationTypeNameMax() {
    return this.mutationTypeNameMax;
  }

  public void setMutationTypeNameMax(String mutationTypeNameMax) {
    this.mutationTypeNameMax = mutationTypeNameMax;
  }

  public String getMutationTypeNameMin() {
    return this.mutationTypeNameMin;
  }

  public void setMutationTypeNameMin(String mutationTypeNameMin) {
    this.mutationTypeNameMin = mutationTypeNameMin;
  }

  public Integer getSubscriptionTypeNameCount() {
    return this.subscriptionTypeNameCount;
  }

  public void setSubscriptionTypeNameCount(Integer subscriptionTypeNameCount) {
    this.subscriptionTypeNameCount = subscriptionTypeNameCount;
  }

  public String getSubscriptionTypeNameMax() {
    return this.subscriptionTypeNameMax;
  }

  public void setSubscriptionTypeNameMax(String subscriptionTypeNameMax) {
    this.subscriptionTypeNameMax = subscriptionTypeNameMax;
  }

  public String getSubscriptionTypeNameMin() {
    return this.subscriptionTypeNameMin;
  }

  public void setSubscriptionTypeNameMin(String subscriptionTypeNameMin) {
    this.subscriptionTypeNameMin = subscriptionTypeNameMin;
  }

  public __Type getTypesAggregate() {
    return this.typesAggregate;
  }

  public void setTypesAggregate(__Type typesAggregate) {
    this.typesAggregate = typesAggregate;
  }

  public __Directive getDirectivesAggregate() {
    return this.directivesAggregate;
  }

  public void setDirectivesAggregate(__Directive directivesAggregate) {
    this.directivesAggregate = directivesAggregate;
  }
}
