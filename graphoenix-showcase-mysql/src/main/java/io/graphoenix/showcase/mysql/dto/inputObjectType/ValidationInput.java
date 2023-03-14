package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.inputObjectType.Property;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Name;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class ValidationInput {
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

  private io.graphoenix.core.dto.inputObjectType.ValidationInput items;

  private Integer minItems;

  private Integer maxItems;

  private Boolean uniqueItems;

  private Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> allOf;

  private Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> anyOf;

  private Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> oneOf;

  private io.graphoenix.core.dto.inputObjectType.ValidationInput not;

  private Collection<Property> properties;

  @Name("if")
  private io.graphoenix.core.dto.inputObjectType.ValidationInput _if;

  private io.graphoenix.core.dto.inputObjectType.ValidationInput then;

  @Name("else")
  private io.graphoenix.core.dto.inputObjectType.ValidationInput _else;

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

  public io.graphoenix.core.dto.inputObjectType.ValidationInput getItems() {
    return this.items;
  }

  public void setItems(io.graphoenix.core.dto.inputObjectType.ValidationInput items) {
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

  public Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> getAllOf() {
    return this.allOf;
  }

  public void setAllOf(Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> allOf) {
    this.allOf = allOf;
  }

  public Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> getAnyOf() {
    return this.anyOf;
  }

  public void setAnyOf(Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> anyOf) {
    this.anyOf = anyOf;
  }

  public Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> getOneOf() {
    return this.oneOf;
  }

  public void setOneOf(Collection<io.graphoenix.core.dto.inputObjectType.ValidationInput> oneOf) {
    this.oneOf = oneOf;
  }

  public io.graphoenix.core.dto.inputObjectType.ValidationInput getNot() {
    return this.not;
  }

  public void setNot(io.graphoenix.core.dto.inputObjectType.ValidationInput not) {
    this.not = not;
  }

  public Collection<Property> getProperties() {
    return this.properties;
  }

  public void setProperties(Collection<Property> properties) {
    this.properties = properties;
  }

  public io.graphoenix.core.dto.inputObjectType.ValidationInput get_if() {
    return this._if;
  }

  public void set_if(io.graphoenix.core.dto.inputObjectType.ValidationInput _if) {
    this._if = _if;
  }

  public io.graphoenix.core.dto.inputObjectType.ValidationInput getThen() {
    return this.then;
  }

  public void setThen(io.graphoenix.core.dto.inputObjectType.ValidationInput then) {
    this.then = then;
  }

  public io.graphoenix.core.dto.inputObjectType.ValidationInput get_else() {
    return this._else;
  }

  public void set_else(io.graphoenix.core.dto.inputObjectType.ValidationInput _else) {
    this._else = _else;
  }

  public Collection<Property> getDependentRequired() {
    return this.dependentRequired;
  }

  public void setDependentRequired(Collection<Property> dependentRequired) {
    this.dependentRequired = dependentRequired;
  }
}
