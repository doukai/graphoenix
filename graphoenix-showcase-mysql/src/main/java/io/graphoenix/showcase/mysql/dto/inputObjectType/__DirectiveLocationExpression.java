package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __DirectiveLocationExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Collection<__DirectiveLocation> in;

  private __DirectiveLocation val;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Collection<__DirectiveLocation> getIn() {
    return this.in;
  }

  public void setIn(Collection<__DirectiveLocation> in) {
    this.in = in;
  }

  public __DirectiveLocation getVal() {
    return this.val;
  }

  public void setVal(__DirectiveLocation val) {
    this.val = val;
  }
}
