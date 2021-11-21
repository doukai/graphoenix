package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import java.lang.String;
import java.util.Set;

public class StringExpression {
  private Operator opr;

  private String val;

  private Set<String> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public String getVal() {
    return this.val;
  }

  public void setVal(String val) {
    this.val = val;
  }

  public Set<String> getIn() {
    return this.in;
  }

  public void setIn(Set<String> in) {
    this.in = in;
  }
}
