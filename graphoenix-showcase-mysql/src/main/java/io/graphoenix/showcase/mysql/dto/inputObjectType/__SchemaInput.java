package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __SchemaInput {
  private String subscriptionTypeName;

  @NonNull
  private Collection<__DirectiveInput> directives;

  @NonNull
  private __TypeInput queryType;

  private String id;

  private Integer version;

  private __TypeInput mutationType;

  private String mutationTypeName;

  private String queryTypeName;

  private __TypeInput subscriptionType;

  private Boolean isDeprecated;

  @NonNull
  private Collection<__TypeInput> types;

  public String getSubscriptionTypeName() {
    return this.subscriptionTypeName;
  }

  public void setSubscriptionTypeName(String subscriptionTypeName) {
    this.subscriptionTypeName = subscriptionTypeName;
  }

  public Collection<__DirectiveInput> getDirectives() {
    return this.directives;
  }

  public void setDirectives(Collection<__DirectiveInput> directives) {
    this.directives = directives;
  }

  public __TypeInput getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__TypeInput queryType) {
    this.queryType = queryType;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public __TypeInput getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__TypeInput mutationType) {
    this.mutationType = mutationType;
  }

  public String getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(String mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public String getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(String queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public __TypeInput getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__TypeInput subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Collection<__TypeInput> getTypes() {
    return this.types;
  }

  public void setTypes(Collection<__TypeInput> types) {
    this.types = types;
  }
}
