package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __SchemaExpression {
  private IDExpression id;

  private StringExpression queryTypeName;

  private StringExpression mutationTypeName;

  private StringExpression subscriptionTypeName;

  private __TypeExpression types;

  private __TypeExpression queryType;

  private __TypeExpression mutationType;

  private __TypeExpression subscriptionType;

  private __DirectiveExpression directives;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__SchemaExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public StringExpression getQueryTypeName() {
    return this.queryTypeName;
  }

  public void setQueryTypeName(StringExpression queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public StringExpression getMutationTypeName() {
    return this.mutationTypeName;
  }

  public void setMutationTypeName(StringExpression mutationTypeName) {
    this.mutationTypeName = mutationTypeName;
  }

  public StringExpression getSubscriptionTypeName() {
    return this.subscriptionTypeName;
  }

  public void setSubscriptionTypeName(StringExpression subscriptionTypeName) {
    this.subscriptionTypeName = subscriptionTypeName;
  }

  public __TypeExpression getTypes() {
    return this.types;
  }

  public void setTypes(__TypeExpression types) {
    this.types = types;
  }

  public __TypeExpression getQueryType() {
    return this.queryType;
  }

  public void setQueryType(__TypeExpression queryType) {
    this.queryType = queryType;
  }

  public __TypeExpression getMutationType() {
    return this.mutationType;
  }

  public void setMutationType(__TypeExpression mutationType) {
    this.mutationType = mutationType;
  }

  public __TypeExpression getSubscriptionType() {
    return this.subscriptionType;
  }

  public void setSubscriptionType(__TypeExpression subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public __DirectiveExpression getDirectives() {
    return this.directives;
  }

  public void setDirectives(__DirectiveExpression directives) {
    this.directives = directives;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public StringExpression get__Typename() {
    return this.__typename;
  }

  public void set__Typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<__SchemaExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__SchemaExpression> exs) {
    this.exs = exs;
  }
}
