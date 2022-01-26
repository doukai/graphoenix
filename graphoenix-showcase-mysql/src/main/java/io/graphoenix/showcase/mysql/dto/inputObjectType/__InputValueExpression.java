package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __InputValueExpression {
  private IntExpression version;

  private StringExpression directiveName;

  private Collection<__InputValueExpression> exs;

  private StringExpression typeName;

  private IDExpression id;

  private __TypeExpression type;

  private StringExpression defaultValue;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression name;

  private StringExpression description;

  private IntExpression fieldId;

  @DefaultValue("AND")
  private Conditional cond;

  private StringExpression ofTypeName;

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

  public Collection<__InputValueExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__InputValueExpression> exs) {
    this.exs = exs;
  }

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public __TypeExpression getType() {
    return this.type;
  }

  public void setType(__TypeExpression type) {
    this.type = type;
  }

  public StringExpression getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(StringExpression defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public IntExpression getFieldId() {
    return this.fieldId;
  }

  public void setFieldId(IntExpression fieldId) {
    this.fieldId = fieldId;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public StringExpression getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(StringExpression ofTypeName) {
    this.ofTypeName = ofTypeName;
  }
}
