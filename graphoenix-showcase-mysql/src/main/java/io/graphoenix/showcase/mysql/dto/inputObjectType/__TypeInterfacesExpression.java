package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __TypeInterfacesExpression {
  private StringExpression id;

  private StringExpression typeName;

  private StringExpression interfaceName;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__TypeInterfacesExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
    this.id = id;
  }

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public StringExpression getInterfaceName() {
    return this.interfaceName;
  }

  public void setInterfaceName(StringExpression interfaceName) {
    this.interfaceName = interfaceName;
  }

  public StringExpression get__typename() {
    return this.__typename;
  }

  public void set__typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<__TypeInterfacesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__TypeInterfacesExpression> exs) {
    this.exs = exs;
  }
}
