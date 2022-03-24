package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __FieldArgumentSet {
  private __FieldArgument id;

  private __FieldArgument name;

  private __FieldArgument typeName;

  private __FieldArgument ofTypeName;

  private __FieldArgument description;

  private __TypeArgumentSet type;

  private __FieldArgument deprecationReason;

  private __FieldArgument version;

  private __FieldArgument isDeprecated;

  private __FieldArgument __typename;

  public __FieldArgument getId() {
    return this.id;
  }

  public void setId(__FieldArgument id) {
    this.id = id;
  }

  public __FieldArgument getName() {
    return this.name;
  }

  public void setName(__FieldArgument name) {
    this.name = name;
  }

  public __FieldArgument getTypeName() {
    return this.typeName;
  }

  public void setTypeName(__FieldArgument typeName) {
    this.typeName = typeName;
  }

  public __FieldArgument getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(__FieldArgument ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __FieldArgument getDescription() {
    return this.description;
  }

  public void setDescription(__FieldArgument description) {
    this.description = description;
  }

  public __TypeArgumentSet getType() {
    return this.type;
  }

  public void setType(__TypeArgumentSet type) {
    this.type = type;
  }

  public __FieldArgument getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(__FieldArgument deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public __FieldArgument getVersion() {
    return this.version;
  }

  public void setVersion(__FieldArgument version) {
    this.version = version;
  }

  public __FieldArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__FieldArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __FieldArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__FieldArgument __typename) {
    this.__typename = __typename;
  }
}
