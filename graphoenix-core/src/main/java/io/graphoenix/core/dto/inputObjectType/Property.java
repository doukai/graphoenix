package io.graphoenix.core.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
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
