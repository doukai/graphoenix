package io.graphoenix.core.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Func;
import io.graphoenix.core.dto.enumType.Operator;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class FuncExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Func val;

  private Collection<Func> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Func getVal() {
    return this.val;
  }

  public void setVal(Func val) {
    this.val = val;
  }

  public Collection<Func> getIn() {
    return this.in;
  }

  public void setIn(Collection<Func> in) {
    this.in = in;
  }
}
