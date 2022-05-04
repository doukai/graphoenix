package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.RoleType;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class RoleTypeExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private RoleType val;

  private Collection<RoleType> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public RoleType getVal() {
    return this.val;
  }

  public void setVal(RoleType val) {
    this.val = val;
  }

  public Collection<RoleType> getIn() {
    return this.in;
  }

  public void setIn(Collection<RoleType> in) {
    this.in = in;
  }
}