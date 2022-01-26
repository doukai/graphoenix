package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.lang.Float;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class FloatExpression {
  private Collection<Float> in;

  private Float val;

  @DefaultValue("EQ")
  private Operator opr;

  public Collection<Float> getIn() {
    return this.in;
  }

  public void setIn(Collection<Float> in) {
    this.in = in;
  }

  public Float getVal() {
    return this.val;
  }

  public void setVal(Float val) {
    this.val = val;
  }

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }
}
