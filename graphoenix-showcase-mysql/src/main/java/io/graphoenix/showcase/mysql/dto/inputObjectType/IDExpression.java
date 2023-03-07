package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.String;
import java.util.Collection;
import null.dto.enumType.Operator;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class IDExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private String val;

  private Collection<String> in;

  @DefaultValue("false")
  private Boolean skipNull;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public String getVal() {
    return this.val;
  }

  public void setVal(String val) {
    this.val = val;
  }

  public Collection<String> getIn() {
    return this.in;
  }

  public void setIn(Collection<String> in) {
    this.in = in;
  }

  public Boolean getSkipNull() {
    return this.skipNull;
  }

  public void setSkipNull(Boolean skipNull) {
    this.skipNull = skipNull;
  }
}
