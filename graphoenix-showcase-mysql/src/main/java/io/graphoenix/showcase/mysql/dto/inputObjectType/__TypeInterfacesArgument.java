package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeInterfacesArgument {
  private __TypeInterfacesArgumentSet args;

  private Sort sort;

  public __TypeInterfacesArgumentSet getArgs() {
    return this.args;
  }

  public void setArgs(__TypeInterfacesArgumentSet args) {
    this.args = args;
  }

  public Sort getSort() {
    return this.sort;
  }

  public void setSort(Sort sort) {
    this.sort = sort;
  }
}
