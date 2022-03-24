package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypePossibleTypesArgumentSet {
  private __TypePossibleTypesArgument id;

  private __TypePossibleTypesArgument typeName;

  private __TypePossibleTypesArgument possibleTypeName;

  private __TypePossibleTypesArgument version;

  private __TypePossibleTypesArgument isDeprecated;

  private __TypePossibleTypesArgument __typename;

  public __TypePossibleTypesArgument getId() {
    return this.id;
  }

  public void setId(__TypePossibleTypesArgument id) {
    this.id = id;
  }

  public __TypePossibleTypesArgument getTypeName() {
    return this.typeName;
  }

  public void setTypeName(__TypePossibleTypesArgument typeName) {
    this.typeName = typeName;
  }

  public __TypePossibleTypesArgument getPossibleTypeName() {
    return this.possibleTypeName;
  }

  public void setPossibleTypeName(__TypePossibleTypesArgument possibleTypeName) {
    this.possibleTypeName = possibleTypeName;
  }

  public __TypePossibleTypesArgument getVersion() {
    return this.version;
  }

  public void setVersion(__TypePossibleTypesArgument version) {
    this.version = version;
  }

  public __TypePossibleTypesArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__TypePossibleTypesArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __TypePossibleTypesArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__TypePossibleTypesArgument __typename) {
    this.__typename = __typename;
  }
}
