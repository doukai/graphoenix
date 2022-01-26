package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class IntExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Collection<Integer> in;

  private Integer val;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Collection<Integer> getIn() {
    return this.in;
  }

  public void setIn(Collection<Integer> in) {
    this.in = in;
  }

  public Integer getVal() {
    return this.val;
  }

  public void setVal(Integer val) {
    this.val = val;
  }
}
