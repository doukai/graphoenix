package io.graphoenix.structure.dto.interfaceType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Interface;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface NamedStruct {
  String name = null;

  String description = null;

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);
}
