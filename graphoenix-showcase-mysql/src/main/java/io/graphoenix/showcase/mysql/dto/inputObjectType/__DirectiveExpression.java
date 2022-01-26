package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __DirectiveExpression {
  private Boolean onFragment;

  private StringExpression description;

  private Boolean onOperation;

  @DefaultValue("AND")
  private Conditional cond;

  private __DirectiveLocationExpression locations;

  private IntExpression version;

  private Collection<__DirectiveExpression> exs;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private __InputValueExpression args;

  private IDExpression name;

  private Boolean onField;

  private IntExpression schemaId;

  public Boolean getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(Boolean onFragment) {
    this.onFragment = onFragment;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public Boolean getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(Boolean onOperation) {
    this.onOperation = onOperation;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public __DirectiveLocationExpression getLocations() {
    return this.locations;
  }

  public void setLocations(__DirectiveLocationExpression locations) {
    this.locations = locations;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public Collection<__DirectiveExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__DirectiveExpression> exs) {
    this.exs = exs;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public __InputValueExpression getArgs() {
    return this.args;
  }

  public void setArgs(__InputValueExpression args) {
    this.args = args;
  }

  public IDExpression getName() {
    return this.name;
  }

  public void setName(IDExpression name) {
    this.name = name;
  }

  public Boolean getOnField() {
    return this.onField;
  }

  public void setOnField(Boolean onField) {
    this.onField = onField;
  }

  public IntExpression getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(IntExpression schemaId) {
    this.schemaId = schemaId;
  }
}
