package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class ConditionalExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Collection<Conditional> in;

  private Conditional val;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Collection<Conditional> getIn() {
    return this.in;
  }

  public void setIn(Collection<Conditional> in) {
    this.in = in;
  }

  public Conditional getVal() {
    return this.val;
  }

  public void setVal(Conditional val) {
    this.val = val;
  }
}
