package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import null.dto.enumType.Operator;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class OperatorExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Operator val;

  private Collection<Operator> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Operator getVal() {
    return this.val;
  }

  public void setVal(Operator val) {
    this.val = val;
  }

  public Collection<Operator> getIn() {
    return this.in;
  }

  public void setIn(Collection<Operator> in) {
    this.in = in;
  }
}
