package io.graphoenix.showcase.mysql.dto.inputObjectType;

import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeInterfacesArgumentSet {
  private __TypeInterfacesArgument id;

  private __TypeInterfacesArgument typeName;

  private __TypeInterfacesArgument interfaceName;

  private __TypeInterfacesArgument version;

  private __TypeInterfacesArgument isDeprecated;

  private __TypeInterfacesArgument __typename;

  public __TypeInterfacesArgument getId() {
    return this.id;
  }

  public void setId(__TypeInterfacesArgument id) {
    this.id = id;
  }

  public __TypeInterfacesArgument getTypeName() {
    return this.typeName;
  }

  public void setTypeName(__TypeInterfacesArgument typeName) {
    this.typeName = typeName;
  }

  public __TypeInterfacesArgument getInterfaceName() {
    return this.interfaceName;
  }

  public void setInterfaceName(__TypeInterfacesArgument interfaceName) {
    this.interfaceName = interfaceName;
  }

  public __TypeInterfacesArgument getVersion() {
    return this.version;
  }

  public void setVersion(__TypeInterfacesArgument version) {
    this.version = version;
  }

  public __TypeInterfacesArgument getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(__TypeInterfacesArgument isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public __TypeInterfacesArgument get__Typename() {
    return this.__typename;
  }

  public void set__Typename(__TypeInterfacesArgument __typename) {
    this.__typename = __typename;
  }
}
