package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __SchemaOrderBy {
  private Sort id;

  private Sort queryTypeName;

  private Sort mutationTypeName;

  private Sort subscriptionTypeName;

  private Sort version;

  private Sort isDeprecated;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(Sort queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public Sort getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(Sort mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public Sort getSubscriptionTypeName() {
    return this.subscriptionTypeName;
  }

  public void setSubscriptionTypeName(Sort subscriptionTypeName) {
    this.subscriptionTypeName = subscriptionTypeName;
  }

  public Sort getVersion() {
    return this.version;
  }

  public void setVersion(Sort version) {
    this.version = version;
  }

  public Sort getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Sort isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Sort get__Typename() {
    return this.__typename;
  }

  public void set__Typename(Sort __typename) {
    this.__typename = __typename;
  }
}
