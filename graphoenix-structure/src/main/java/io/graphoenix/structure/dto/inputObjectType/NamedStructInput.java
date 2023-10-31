package io.graphoenix.structure.dto.inputObjectType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface NamedStructInput {
  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);
}
