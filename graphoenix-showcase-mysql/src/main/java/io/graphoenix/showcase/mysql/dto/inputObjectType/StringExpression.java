package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class StringExpression {
  private Collection<String> in;

  private String val;

  @DefaultValue("EQ")
  private Operator opr;

  public Collection<String> getIn() {
    return this.in;
  }

  public void setIn(Collection<String> in) {
    this.in = in;
  }

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
}
