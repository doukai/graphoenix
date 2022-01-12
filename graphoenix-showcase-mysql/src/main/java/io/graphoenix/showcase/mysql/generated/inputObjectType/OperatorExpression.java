package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import jakarta.annotation.Generated;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class OperatorExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Operator val;

  private Set<Operator> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Operator getVal() {
    return this.val;
  }

  public void setVal(Operator val) {
    this.val = val;
  }

  public Set<Operator> getIn() {
    return this.in;
  }

  public void setIn(Set<Operator> in) {
    this.in = in;
  }
}
