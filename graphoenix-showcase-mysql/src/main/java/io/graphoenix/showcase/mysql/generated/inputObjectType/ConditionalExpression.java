package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Conditional;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import jakarta.annotation.Generated;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class ConditionalExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Conditional val;

  private Set<Conditional> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Conditional getVal() {
    return this.val;
  }

  public void setVal(Conditional val) {
    this.val = val;
  }

  public Set<Conditional> getIn() {
    return this.in;
  }

  public void setIn(Set<Conditional> in) {
    this.in = in;
  }
}
