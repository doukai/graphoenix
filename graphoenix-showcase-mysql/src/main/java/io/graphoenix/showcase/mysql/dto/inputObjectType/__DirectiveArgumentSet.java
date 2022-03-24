package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __DirectiveArgumentSet {
  private __DirectiveArgument name;

  private __DirectiveArgument schemaId;

  private __DirectiveArgument description;

  private __DirectiveArgument onOperation;

  private __DirectiveArgument onFragment;

  private __DirectiveArgument onField;

  private __DirectiveArgument version;

  private __DirectiveArgument isDeprecated;

  private __DirectiveArgument __typename;

  public __DirectiveArgument getName() {
    return this.name;
  }

  public void setName(__DirectiveArgument name) {
    this.name = name;
  }

  public __DirectiveArgument getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(__DirectiveArgument schemaId) {
    this.schemaId = schemaId;
  }

  public __DirectiveArgument getDescription() {
    return this.description;
  }

  public void setDescription(__DirectiveArgument description) {
    this.description = description;
  }

  public __DirectiveArgument getOnOperation() {
    return this.onOperation;
  }

  public void setOnOperation(__DirectiveArgument onOperation) {
    this.onOperation = onOperation;
  }

  public __DirectiveArgument getOnFragment() {
    return this.onFragment;
  }

  public void setOnFragment(__DirectiveArgument onFragment) {
    this.onFragment = onFragment;
  }

  public __DirectiveArgument getOnField() {
    return this.onField;
  }

  public void setOnField(__DirectiveArgument onField) {
    this.onField = onField;
  }

  public __DirectiveArgument getVersion() {
    return this.version;
  }

  public void setVersion(__DirectiveArgument version) {
    this.version = version;
  }

  public __DirectiveArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__DirectiveArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __DirectiveArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__DirectiveArgument __typename) {
    this.__typename = __typename;
  }
}
