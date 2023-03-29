package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __SchemaInput {
  private String id;

  private String queryTypeName;

  private String mutationTypeName;

  private String subscriptionTypeName;

  private Collection<__TypeInput> types;

  private __TypeInput queryType;

  private __TypeInput mutationType;

  private __TypeInput subscriptionType;

  private Collection<__DirectiveInput> directives;

  @DefaultValue("\"__Schema\"")
  private String __typename;

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

  public Collection<__TypeInput> getTypes() {
    return this.types;
  }

  public void setTypes(Collection<__TypeInput> types) {
    this.types = types;
  }

  public __TypeInput getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__TypeInput queryType) {
    this.queryType = queryType;
  }

  public __TypeInput getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__TypeInput mutationType) {
    this.mutationType = mutationType;
  }

  public __TypeInput getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__TypeInput subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public Collection<__DirectiveInput> getDirectives() {
    return this.directives;
  }

  public void setDirectives(Collection<__DirectiveInput> directives) {
    this.directives = directives;
  }

  public String get__typename() {
    return this.__typename;
  }

  public void set__typename(String __typename) {
    this.__typename = __typename;
  }
}
