package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypePossibleTypesExpression {
  private IDExpression id;

  private StringExpression typeName;

  private StringExpression possibleTypeName;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__TypePossibleTypesExpression> exs;

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public StringExpression getPossibleTypeName() {
    return this.possibleTypeName;
  }

  public void setPossibleTypeName(StringExpression possibleTypeName) {
    this.possibleTypeName = possibleTypeName;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<__TypePossibleTypesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__TypePossibleTypesExpression> exs) {
    this.exs = exs;
  }
}
