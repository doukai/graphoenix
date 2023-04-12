package io.graphoenix.core.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Conditional;
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
public class ConditionalExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Conditional val;

  private Collection<Conditional> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Conditional getVal() {
    return this.val;
  }

  public void setVal(Conditional val) {
    this.val = val;
  }

  public Collection<Conditional> getIn() {
    return this.in;
  }

  public void setIn(Collection<Conditional> in) {
    this.in = in;
  }
}
