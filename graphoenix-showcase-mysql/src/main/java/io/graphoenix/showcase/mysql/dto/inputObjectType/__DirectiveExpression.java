package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __DirectiveExpression {
  private StringExpression name;

  private IntExpression schemaId;

  private StringExpression description;

  private __DirectiveLocationExpression locations;

  private __InputValueExpression args;

  private BooleanExpression onOperation;

  private BooleanExpression onFragment;

  private BooleanExpression onField;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__DirectiveExpression> exs;

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public IntExpression getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(IntExpression schemaId) {
    this.schemaId = schemaId;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public __DirectiveLocationExpression getLocations() {
    return this.locations;
  }

  public void setLocations(__DirectiveLocationExpression locations) {
    this.locations = locations;
  }

  public __InputValueExpression getArgs() {
    return this.args;
  }

  public void setArgs(__InputValueExpression args) {
    this.args = args;
  }

  public BooleanExpression getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(BooleanExpression onOperation) {
    this.onOperation = onOperation;
  }

  public BooleanExpression getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(BooleanExpression onFragment) {
    this.onFragment = onFragment;
  }

  public BooleanExpression getOnField() {
    return this.onField;
  }

  public void setOnField(BooleanExpression onField) {
    this.onField = onField;
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

  public Collection<__DirectiveExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__DirectiveExpression> exs) {
    this.exs = exs;
  }
}
