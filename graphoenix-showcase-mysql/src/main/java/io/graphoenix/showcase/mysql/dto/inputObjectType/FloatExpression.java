package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Float;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class FloatExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Float val;

  private Collection<Float> in;

  @DefaultValue("false")
  private Boolean skipNull;

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

  public Boolean getSkipNull() {
    return this.skipNull;
  }

  public void setSkipNull(Boolean skipNull) {
    this.skipNull = skipNull;
  }
}
