package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class IDExpression {
  private String val;

  @DefaultValue("EQ")
  private Operator opr;

  private Collection<String> in;

  public String getVal() {
    return this.val;
  }

  public void setVal(String val) {
    this.val = val;
  }

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Collection<String> getIn() {
    return this.in;
  }

  public void setIn(Collection<String> in) {
    this.in = in;
  }
}
