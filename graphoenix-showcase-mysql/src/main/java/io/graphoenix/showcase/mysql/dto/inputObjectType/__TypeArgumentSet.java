package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeArgumentSet {
  private __TypeArgument name;

  private __TypeArgument schemaId;

  private __TypeArgument kind;

  private __TypeArgument description;

  private __TypeArgument ofTypeName;

  private __TypeArgumentSet ofType;

  private __TypeArgument version;

  private __TypeArgument isDeprecated;

  private __TypeArgument __typename;

  public __TypeArgument getName() {
    return this.name;
  }

  public void setName(__TypeArgument name) {
    this.name = name;
  }

  public __TypeArgument getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(__TypeArgument schemaId) {
    this.schemaId = schemaId;
  }

  public __TypeArgument getKind() {
    return this.kind;
  }

  public void setKind(__TypeArgument kind) {
    this.kind = kind;
  }

  public __TypeArgument getDescription() {
    return this.description;
  }

  public void setDescription(__TypeArgument description) {
    this.description = description;
  }

  public __TypeArgument getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(__TypeArgument ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __TypeArgumentSet getOfType() {
    return this.ofType;
  }

  public void setOfType(__TypeArgumentSet ofType) {
    this.ofType = ofType;
  }

  public __TypeArgument getVersion() {
    return this.version;
  }

  public void setVersion(__TypeArgument version) {
    this.version = version;
  }

  public __TypeArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__TypeArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __TypeArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__TypeArgument __typename) {
    this.__typename = __typename;
  }
}
