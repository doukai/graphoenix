package io.graphoenix.showcase.user.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Operator;
import io.graphoenix.showcase.user.dto.enumType.RoleType;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class RoleTypeExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private RoleType val;

  private Collection<RoleType> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public RoleType getVal() {
    return this.val;
  }

  public void setVal(RoleType val) {
    this.val = val;
  }

  public Collection<RoleType> getIn() {
    return this.in;
  }

  public void setIn(Collection<RoleType> in) {
    this.in = in;
  }
}