package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Function;
import io.graphoenix.core.dto.enumType.Operator;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class FunctionExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Function val;

  private Collection<Function> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Function getVal() {
    return this.val;
  }

  public void setVal(Function val) {
    this.val = val;
  }

  public Collection<Function> getIn() {
    return this.in;
  }

  public void setIn(Collection<Function> in) {
    this.in = in;
  }
}
