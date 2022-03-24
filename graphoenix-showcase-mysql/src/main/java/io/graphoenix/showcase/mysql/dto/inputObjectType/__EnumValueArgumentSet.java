package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __EnumValueArgumentSet {
  private __EnumValueArgument id;

  private __EnumValueArgument name;

  private __EnumValueArgument ofTypeName;

  private __EnumValueArgument description;

  private __EnumValueArgument deprecationReason;

  private __EnumValueArgument version;

  private __EnumValueArgument isDeprecated;

  private __EnumValueArgument __typename;

  public __EnumValueArgument getId() {
    return this.id;
  }

  public void setId(__EnumValueArgument id) {
    this.id = id;
  }

  public __EnumValueArgument getName() {
    return this.name;
  }

  public void setName(__EnumValueArgument name) {
    this.name = name;
  }

  public __EnumValueArgument getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(__EnumValueArgument ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __EnumValueArgument getDescription() {
    return this.description;
  }

  public void setDescription(__EnumValueArgument description) {
    this.description = description;
  }

  public __EnumValueArgument getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(__EnumValueArgument deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public __EnumValueArgument getVersion() {
    return this.version;
  }

  public void setVersion(__EnumValueArgument version) {
    this.version = version;
  }

  public __EnumValueArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__EnumValueArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __EnumValueArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__EnumValueArgument __typename) {
    this.__typename = __typename;
  }
}
