package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeInterfacesExpression {
  private StringExpression typeName;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression interfaceName;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__TypeInterfacesExpression> exs;

  private IntExpression version;

  private IDExpression id;

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public StringExpression getInterfaceName() {
    return this.interfaceName;
  }

  public void setInterfaceName(StringExpression interfaceName) {
    this.interfaceName = interfaceName;
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

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }
}
