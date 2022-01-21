package io.graphoenix.showcase.mysql.generated.inputObjectType;

import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import jakarta.annotation.Generated;
import java.lang.Float;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class FloatExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Float val;

  private Collection<Float> in;

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

  public Collection<Float> getIn() {
    return this.in;
  }

  public void setIn(Collection<Float> in) {
    this.in = in;
  }
}
