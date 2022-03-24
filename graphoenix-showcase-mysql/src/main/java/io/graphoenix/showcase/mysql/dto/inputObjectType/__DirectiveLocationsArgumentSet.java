package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __DirectiveLocationsArgumentSet {
  private __DirectiveLocationsArgument id;

  private __DirectiveLocationsArgument directiveName;

  private __DirectiveLocationsArgument directiveLocation;

  private __DirectiveLocationsArgument version;

  private __DirectiveLocationsArgument isDeprecated;

  private __DirectiveLocationsArgument __typename;

  public __DirectiveLocationsArgument getId() {
    return this.id;
  }

  public void setId(__DirectiveLocationsArgument id) {
    this.id = id;
  }

  public __DirectiveLocationsArgument getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(__DirectiveLocationsArgument directiveName) {
    this.directiveName = directiveName;
  }

  public __DirectiveLocationsArgument getDirectiveLocation() {
    return this.directiveLocation;
  }

  public void setDirectiveLocation(__DirectiveLocationsArgument directiveLocation) {
    this.directiveLocation = directiveLocation;
  }

  public __DirectiveLocationsArgument getVersion() {
    return this.version;
  }

  public void setVersion(__DirectiveLocationsArgument version) {
    this.version = version;
  }

  public __DirectiveLocationsArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__DirectiveLocationsArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __DirectiveLocationsArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__DirectiveLocationsArgument __typename) {
    this.__typename = __typename;
  }
}
