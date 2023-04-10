package io.graphoenix.showcase.order.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.order.dto.enumType.Operator;
import io.graphoenix.showcase.order.dto.enumType.__TypeKind;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __TypeKindExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private __TypeKind val;

  private Collection<__TypeKind> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public __TypeKind getVal() {
    return this.val;
  }

  public void setVal(__TypeKind val) {
    this.val = val;
  }

  public Collection<__TypeKind> getIn() {
    return this.in;
  }

  public void setIn(Collection<__TypeKind> in) {
    this.in = in;
  }
}
