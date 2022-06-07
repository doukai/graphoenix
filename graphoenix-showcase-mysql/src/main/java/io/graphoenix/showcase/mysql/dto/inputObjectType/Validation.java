package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Name;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class Validation {
  private Integer minLength;

  private Integer maxLength;

  private String pattern;

  private String format;

  private String contentMediaType;

  private String contentEncoding;

  private Float minimum;

  private Float exclusiveMinimum;

  private Float maximum;

  private Float exclusiveMaximum;

  private Float multipleOf;

  @Name("const")
  private String _const;

  @Name("enum")
  private Collection<String> _enum;

  private Validation items;

  private Integer minItems;

  private Integer maxItems;

  private Boolean uniqueItems;

  private Collection<Validation> allOf;

  private Collection<Validation> anyOf;

  private Collection<Validation> oneOf;

  private Validation not;

  private Collection<Property> properties;

  @Name("if")
  private Validation _if;

  private Validation then;

  @Name("else")
  private Validation _else;

  private Collection<Property> dependentRequired;

  public Integer getMinLength() {
    return this.minLength;
  }

  public void setMinLength(Integer minLength) {
    this.minLength = minLength;
  }

  public Integer getMaxLength() {
    return this.maxLength;
  }

  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  public String getPattern() {
    return this.pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getFormat() {
    return this.format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getContentMediaType() {
    return this.contentMediaType;
  }

  public void setContentMediaType(String contentMediaType) {
    this.contentMediaType = contentMediaType;
  }

  public String getContentEncoding() {
    return this.contentEncoding;
  }

  public void setContentEncoding(String contentEncoding) {
    this.contentEncoding = contentEncoding;
  }

  public Float getMinimum() {
    return this.minimum;
  }

  public void setMinimum(Float minimum) {
    this.minimum = minimum;
  }

  public Float getExclusiveMinimum() {
    return this.exclusiveMinimum;
  }

  public void setExclusiveMinimum(Float exclusiveMinimum) {
    this.exclusiveMinimum = exclusiveMinimum;
  }

  public Float getMaximum() {
    return this.maximum;
  }

  public void setMaximum(Float maximum) {
    this.maximum = maximum;
  }

  public Float getExclusiveMaximum() {
    return this.exclusiveMaximum;
  }

  public void setExclusiveMaximum(Float exclusiveMaximum) {
    this.exclusiveMaximum = exclusiveMaximum;
  }

  public Float getMultipleOf() {
    return this.multipleOf;
  }

  public void setMultipleOf(Float multipleOf) {
    this.multipleOf = multipleOf;
  }

  public String get_const() {
    return this._const;
  }

  public void set_const(String _const) {
    this._const = _const;
  }

  public Collection<String> get_enum() {
    return this._enum;
  }

  public void set_enum(Collection<String> _enum) {
    this._enum = _enum;
  }

  public Validation getItems() {
    return this.items;
  }

  public void setItems(Validation items) {
    this.items = items;
  }

  public Integer getMinItems() {
    return this.minItems;
  }

  public void setMinItems(Integer minItems) {
    this.minItems = minItems;
  }

  public Integer getMaxItems() {
    return this.maxItems;
  }

  public void setMaxItems(Integer maxItems) {
    this.maxItems = maxItems;
  }

  public Boolean getUniqueItems() {
    return this.uniqueItems;
  }

  public void setUniqueItems(Boolean uniqueItems) {
    this.uniqueItems = uniqueItems;
  }

  public Collection<Validation> getAllOf() {
    return this.allOf;
  }

  public void setAllOf(Collection<Validation> allOf) {
    this.allOf = allOf;
  }

  public Collection<Validation> getAnyOf() {
    return this.anyOf;
  }

  public void setAnyOf(Collection<Validation> anyOf) {
    this.anyOf = anyOf;
  }

  public Collection<Validation> getOneOf() {
    return this.oneOf;
  }

  public void setOneOf(Collection<Validation> oneOf) {
    this.oneOf = oneOf;
  }

  public Validation getNot() {
    return this.not;
  }

  public void setNot(Validation not) {
    this.not = not;
  }

  public Collection<Property> getProperties() {
    return this.properties;
  }

  public void setProperties(Collection<Property> properties) {
    this.properties = properties;
  }

  public Validation get_if() {
    return this._if;
  }

  public void set_if(Validation _if) {
    this._if = _if;
  }

  public Validation getThen() {
    return this.then;
  }

  public void setThen(Validation then) {
    this.then = then;
  }

  public Validation get_else() {
    return this._else;
  }

  public void set_else(Validation _else) {
    this._else = _else;
  }

  public Collection<Property> getDependentRequired() {
    return this.dependentRequired;
  }

  public void setDependentRequired(Collection<Property> dependentRequired) {
    this.dependentRequired = dependentRequired;
  }
}
