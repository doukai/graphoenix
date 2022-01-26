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
  private Collection<__InputValueExpression> exs;

  private StringExpression directiveName;

  @DefaultValue("AND")
  private Conditional cond;

  private IntExpression fieldId;

  private StringExpression defaultValue;

  private IntExpression version;

  private StringExpression typeName;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private StringExpression description;

  private IDExpression id;

  private StringExpression ofTypeName;

  private __TypeExpression type;

  private StringExpression name;

  public Collection<__InputValueExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__InputValueExpression> exs) {
    this.exs = exs;
  }

  public StringExpression getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(StringExpression directiveName) {
    this.directiveName = directiveName;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public IntExpression getFieldId() {
    return this.fieldId;
  }

  public void setFieldId(IntExpression fieldId) {
    this.fieldId = fieldId;
  }

  public StringExpression getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(StringExpression defaultValue) {
    this.defaultValue = defaultValue;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

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

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public IDExpression getId() {
    return this.id;
  }

  public void setId(IDExpression id) {
    this.id = id;
  }

  public StringExpression getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(StringExpression ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __TypeExpression getType() {
    return this.type;
  }

  public void setType(__TypeExpression type) {
    this.type = type;
  }

  public StringExpression getName() {
    return this.name;
  }

  public void setName(StringExpression name) {
    this.name = name;
  }
}
