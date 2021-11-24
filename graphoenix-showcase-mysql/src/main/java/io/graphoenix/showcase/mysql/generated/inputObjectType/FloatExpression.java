package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.Float;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class FloatExpression {
  @DefaultValue("=EQ")
  private Operator opr;

  private Float val;

  private Set<Float> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Float getVal() {
    return this.val;
  }

  public void setVal(Float val) {
    this.val = val;
  }

  public Set<Float> getIn() {
    return this.in;
  }

  public void setIn(Set<Float> in) {
    this.in = in;
  }
}
