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
  private Integer version;

  private Boolean isDeprecated;

  private __Type subscriptionType;

  @Id
  private String id;

  private __Type mutationType;

  private String subscriptionTypeName;

  @NonNull
  private Collection<__Type> types;

  private String mutationTypeName;

  @NonNull
  private __Type queryType;

  private String queryTypeName;

  @NonNull
  private Collection<__Directive> directives;

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

  public __Type getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__Type subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public __Type getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__Type mutationType) {
    this.mutationType = mutationType;
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

  public String getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(String mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public __Type getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__Type queryType) {
    this.queryType = queryType;
  }

  public String getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(String queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public Collection<__Directive> getDirectives() {
    return this.directives;
  }

  public void setDirectives(Collection<__Directive> directives) {
    this.directives = directives;
  }
}
