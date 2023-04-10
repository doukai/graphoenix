package io.graphoenix.showcase.user.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.user.dto.enumType.Operator;
import io.graphoenix.showcase.user.dto.enumType.Sex;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class SexExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Sex val;

  private Collection<Sex> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Sex getVal() {
    return this.val;
  }

  public void setVal(Sex val) {
    this.val = val;
  }

  public Collection<Sex> getIn() {
    return this.in;
  }

  public void setIn(Collection<Sex> in) {
    this.in = in;
  }
}
