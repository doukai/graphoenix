package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class Property {
  @NonNull
  private String name;

  private ValidationInput validation;

  private Collection<String> required;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ValidationInput getValidation() {
    return this.validation;
  }

  public void setValidation(ValidationInput validation) {
    this.validation = validation;
  }

  public Collection<String> getRequired() {
    return this.required;
  }

  public void setRequired(Collection<String> required) {
    this.required = required;
  }
}