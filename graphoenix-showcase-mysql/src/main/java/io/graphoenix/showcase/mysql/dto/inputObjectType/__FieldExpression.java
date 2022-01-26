package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __FieldExpression {
  private StringExpression typeName;

  private StringExpression name;

  private __InputValueExpression args;

  private Collection<__FieldExpression> exs;

  @DefaultValue("AND")
  private Conditional cond;

  private StringExpression description;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression deprecationReason;

  private IDExpression id;

  private __TypeExpression type;

  private IntExpression version;

  private StringExpression ofTypeName;

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public __InputValueExpression getArgs() {
    return this.args;
  }

  public void setArgs(__InputValueExpression args) {
    this.args = args;
  }

  public Collection<__FieldExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__FieldExpression> exs) {
    this.exs = exs;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public StringExpression getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(StringExpression deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public __TypeExpression getType() {
    return this.type;
  }

  public void setType(__TypeExpression type) {
    this.type = type;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public StringExpression getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(StringExpression ofTypeName) {
    this.ofTypeName = ofTypeName;
  }
}
