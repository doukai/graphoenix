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
  private IDExpression name;

  private IntExpression schemaId;

  private __TypeKindExpression kind;

  private StringExpression description;

  private __FieldExpression fields;

  private __TypeExpression interfaces;

  private __TypeExpression possibleTypes;

  private __EnumValueExpression enumValues;

  private __InputValueExpression inputFields;

  private StringExpression ofTypeName;

  private __TypeExpression ofType;

  private IntExpression version;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__TypeExpression> exs;

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

  public __TypeKindExpression getKind() {
    return this.kind;
  }

  public void setKind(__TypeKindExpression kind) {
    this.kind = kind;
  }

  public StringExpression getDescription() {
    return this.description;
  }

  public void setDescription(StringExpression description) {
    this.description = description;
  }

  public __FieldExpression getFields() {
    return this.fields;
  }

  public void setFields(__FieldExpression fields) {
    this.fields = fields;
  }

  public __TypeExpression getInterfaces() {
    return this.interfaces;
  }

  public void setInterfaces(__TypeExpression interfaces) {
    this.interfaces = interfaces;
  }

  public __TypeExpression getPossibleTypes() {
    return this.possibleTypes;
  }

  public void setPossibleTypes(__TypeExpression possibleTypes) {
    this.possibleTypes = possibleTypes;
  }

  public __EnumValueExpression getEnumValues() {
    return this.enumValues;
  }

  public void setEnumValues(__EnumValueExpression enumValues) {
    this.enumValues = enumValues;
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

  public __TypeExpression getOfType() {
    return this.ofType;
  }

  public void setOfType(__TypeExpression ofType) {
    this.ofType = ofType;
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

  public Collection<__TypeExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__TypeExpression> exs) {
    this.exs = exs;
  }
}