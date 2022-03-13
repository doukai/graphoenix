package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class PageInfoExpression {
  private Boolean hasPreviousPage;

  private Boolean hasNextPage;

  private StringExpression startCursor;

  private StringExpression endCursor;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<PageInfoExpression> exs;

  public Boolean getHasPreviousPage() {
    return this.hasPreviousPage;
  }

  public void setHasPreviousPage(Boolean hasPreviousPage) {
    this.hasPreviousPage = hasPreviousPage;
  }

  public Boolean getHasNextPage() {
    return this.hasNextPage;
  }

  public void setHasNextPage(Boolean hasNextPage) {
    this.hasNextPage = hasNextPage;
  }

  public StringExpression getStartCursor() {
    return this.startCursor;
  }

  public void setStartCursor(StringExpression startCursor) {
    this.startCursor = startCursor;
  }

  public StringExpression getEndCursor() {
    return this.endCursor;
  }

  public void setEndCursor(StringExpression endCursor) {
    this.endCursor = endCursor;
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

  public StringExpression get__typename() {
    return this.__typename;
  }

  public void set__typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<PageInfoExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<PageInfoExpression> exs) {
    this.exs = exs;
  }
}
