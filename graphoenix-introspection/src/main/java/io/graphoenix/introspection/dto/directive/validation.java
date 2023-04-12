package io.graphoenix.introspection.dto.directive;

import io.graphoenix.core.dto.annotation.Property;
import io.graphoenix.core.dto.annotation.ValidationInput;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.microprofile.graphql.Name;

@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface validation {
  int minLength();

  int maxLength();

  String pattern();

  String format();

  String contentMediaType();

  String contentEncoding();

  float minimum();

  float exclusiveMinimum();

  float maximum();

  float exclusiveMaximum();

  float multipleOf();

  @Name("const")
  String _const();

  @Name("enum")
  String[] _enum();

  ValidationInput items();

  int minItems();

  int maxItems();

  boolean uniqueItems();

  ValidationInput[] allOf();

  ValidationInput[] anyOf();

  ValidationInput[] oneOf();

  ValidationInput not();

  Property[] properties();

  @Name("if")
  ValidationInput _if();

  ValidationInput then();

  @Name("else")
  ValidationInput _else();

  Property[] dependentRequired();
}
