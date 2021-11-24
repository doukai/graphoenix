package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.Integer;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class IDExpression {
  @DefaultValue("=EQ")
  private Operator opr;

  private Integer val;

  private Set<Integer> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Integer getVal() {
    return this.val;
  }

  public void setVal(Integer val) {
    this.val = val;
  }

  public Set<Integer> getIn() {
    return this.in;
  }

  public void setIn(Set<Integer> in) {
    this.in = in;
  }
}
