package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __SchemaArgumentSet {
  private __SchemaArgument id;

  private __SchemaArgument queryTypeName;

  private __SchemaArgument mutationTypeName;

  private __SchemaArgument subscriptionTypeName;

  private __TypeArgumentSet queryType;

  private __TypeArgumentSet mutationType;

  private __TypeArgumentSet subscriptionType;

  private __SchemaArgument version;

  private __SchemaArgument isDeprecated;

  private __SchemaArgument __typename;

  public __SchemaArgument getId() {
    return this.id;
  }

  public void setId(__SchemaArgument id) {
    this.id = id;
  }

  public __SchemaArgument getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(__SchemaArgument queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public __SchemaArgument getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(__SchemaArgument mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public __SchemaArgument getSubscriptionTypeName() {
    return this.subscriptionTypeName;
  }

  public void setSubscriptionTypeName(__SchemaArgument subscriptionTypeName) {
    this.subscriptionTypeName = subscriptionTypeName;
  }

  public __TypeArgumentSet getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__TypeArgumentSet queryType) {
    this.queryType = queryType;
  }

  public __TypeArgumentSet getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__TypeArgumentSet mutationType) {
    this.mutationType = mutationType;
  }

  public __TypeArgumentSet getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__TypeArgumentSet subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public __SchemaArgument getVersion() {
    return this.version;
  }

  public void setVersion(__SchemaArgument version) {
    this.version = version;
  }

  public __SchemaArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__SchemaArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __SchemaArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__SchemaArgument __typename) {
    this.__typename = __typename;
  }
}
