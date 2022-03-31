package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Operator;
import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class SortExpression {
  @DefaultValue("EQ")
  private Operator opr;

  private Sort val;

  private Collection<Sort> in;

  public Operator getOpr() {
    return this.opr;
  }

  public void setOpr(Operator opr) {
    this.opr = opr;
  }

  public Sort getVal() {
    return this.val;
  }

  public void setVal(Sort val) {
    this.val = val;
  }

  public Collection<Sort> getIn() {
    return this.in;
  }

  public void setIn(Collection<Sort> in) {
    this.in = in;
  }
}
