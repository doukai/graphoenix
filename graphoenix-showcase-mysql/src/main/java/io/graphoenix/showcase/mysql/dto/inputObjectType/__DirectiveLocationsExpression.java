package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __DirectiveLocationsExpression {
  @DefaultValue("AND")
  private Conditional cond;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private Collection<__DirectiveLocationsExpression> exs;

  private IDExpression id;

  private IntExpression version;

  private StringExpression directiveName;

  private __DirectiveLocationExpression directiveLocation;

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public Collection<__DirectiveLocationsExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__DirectiveLocationsExpression> exs) {
    this.exs = exs;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public StringExpression getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(StringExpression directiveName) {
    this.directiveName = directiveName;
  }

  public __DirectiveLocationExpression getDirectiveLocation() {
    return this.directiveLocation;
  }

  public void setDirectiveLocation(__DirectiveLocationExpression directiveLocation) {
    this.directiveLocation = directiveLocation;
  }
}
