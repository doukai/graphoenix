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
  private String queryTypeName;

  private __TypeInput subscriptionType;

  private String mutationTypeName;

  private __TypeInput mutationType;

  private Boolean isDeprecated;

  private String id;

  private String subscriptionTypeName;

  @NonNull
  private Collection<__DirectiveInput> directives;

  private Integer version;

  @NonNull
  private __TypeInput queryType;

  @NonNull
  private Collection<__TypeInput> types;

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

  public String getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(String mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public __TypeInput getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__TypeInput mutationType) {
    this.mutationType = mutationType;
  }

  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public Integer getVersion() {
    return this.version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public __TypeInput getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__TypeInput queryType) {
    this.queryType = queryType;
  }

  public Collection<__TypeInput> getTypes() {
    return this.types;
  }

  public void setTypes(Collection<__TypeInput> types) {
    this.types = types;
  }
}
