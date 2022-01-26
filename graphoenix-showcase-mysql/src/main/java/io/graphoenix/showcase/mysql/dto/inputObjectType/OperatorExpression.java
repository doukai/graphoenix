package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OperatorExpression {
  private Collection<Operator> in;

  private Operator val;

  @DefaultValue("EQ")
  private Operator opr;

  public Collection<Operator> getIn() {
    return this.in;
  }

  public void setIn(Collection<Operator> in) {
    this.in = in;
  }

  public Operator getVal() {
    return this.val;
  }

  public void setVal(Operator val) {
    this.val = val;
  }

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }
}
