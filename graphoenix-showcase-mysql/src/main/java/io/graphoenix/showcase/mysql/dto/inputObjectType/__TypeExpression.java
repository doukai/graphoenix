package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
public class __TypeExpression {
  private __TypeExpression possibleTypes;

  private __TypeExpression ofType;

  private __TypeExpression interfaces;

  private __FieldExpression fields;

  private Collection<__TypeExpression> exs;

  @DefaultValue("AND")
  private Conditional cond;

  private __EnumValueExpression enumValues;

  private StringExpression description;

  private __TypeKindExpression kind;

  private __InputValueExpression inputFields;

  private StringExpression ofTypeName;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IDExpression name;

  private IntExpression schemaId;

  private IntExpression version;

  public __TypeExpression getPossibleTypes() {
    return this.possibleTypes;
  }

  public void setPossibleTypes(__TypeExpression possibleTypes) {
    this.possibleTypes = possibleTypes;
  }

  public __TypeExpression getOfType() {
    return this.ofType;
  }

  public void setOfType(__TypeExpression ofType) {
    this.ofType = ofType;
  }

  public __TypeExpression getInterfaces() {
    return this.interfaces;
  }

  public void setInterfaces(__TypeExpression interfaces) {
    this.interfaces = interfaces;
  }

  public __FieldExpression getFields() {
    return this.fields;
  }

  public void setFields(__FieldExpression fields) {
    this.fields = fields;
  }

  public Collection<__TypeExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__TypeExpression> exs) {
    this.exs = exs;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public __EnumValueExpression getEnumValues() {
    return this.enumValues;
  }

  public void setEnumValues(__EnumValueExpression enumValues) {
    this.enumValues = enumValues;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public __TypeKindExpression getKind() {
    return this.kind;
  }

  public void setKind(__TypeKindExpression kind) {
    this.kind = kind;
  }

  public __InputValueExpression getInputFields() {
    return this.inputFields;
  }

  public void setInputFields(__InputValueExpression inputFields) {
    this.inputFields = inputFields;
  }

  public StringExpression getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(StringExpression ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IDExpression getName() {
    return this.name;
  }

  public void setName(IDExpression name) {
    this.name = name;
  }

  public IntExpression getSchemaId() {
    return this.schemaId;
  }

  public void setSchemaId(IntExpression schemaId) {
    this.schemaId = schemaId;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }
}
