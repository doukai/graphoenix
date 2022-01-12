package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import jakarta.annotation.Generated;
import java.util.Set;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class SexExpression {
  @DefaultValue("EQ")
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
