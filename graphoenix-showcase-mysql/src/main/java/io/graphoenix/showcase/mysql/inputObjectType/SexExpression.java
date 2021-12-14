package io.graphoenix.showcase.mysql.inputObjectType;

import io.graphoenix.showcase.mysql.enumType.Operator;
import io.graphoenix.showcase.mysql.enumType.Sex;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
public class SexExpression {
  @DefaultValue("=EQ")
  private Operator opr;

  private Sex val;

  private Set<Sex> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Sex getVal() {
    return this.val;
  }

  public void setVal(Sex val) {
    this.val = val;
  }

  public Set<Sex> getIn() {
    return this.in;
  }

  public void setIn(Set<Sex> in) {
    this.in = in;
  }
}
