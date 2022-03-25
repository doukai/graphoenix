package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Function;
import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class UserRoleArgument {
  private Function name;

  private UserRoleArgumentSet args;

  private Sort sort;

  public Function getName() {
    return this.name;
  }

  public void setName(Function name) {
    this.name = name;
  }

  public UserRoleArgumentSet getArgs() {
    return this.args;
  }

  public void setArgs(UserRoleArgumentSet args) {
    this.args = args;
  }

  public Sort getSort() {
    return this.sort;
  }

  public void setSort(Sort sort) {
    this.sort = sort;
  }
}
