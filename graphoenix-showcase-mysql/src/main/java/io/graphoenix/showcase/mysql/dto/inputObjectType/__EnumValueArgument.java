package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Function;
import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __EnumValueArgument {
  private Function name;

  private __EnumValueArgumentSet args;

  private Sort sort;

  public Function getName() {
    return this.name;
  }

  public void setName(Function name) {
    this.name = name;
  }

  public __EnumValueArgumentSet getArgs() {
    return this.args;
  }

  public void setArgs(__EnumValueArgumentSet args) {
    this.args = args;
  }

  public Sort getSort() {
    return this.sort;
  }

  public void setSort(Sort sort) {
    this.sort = sort;
  }
}
