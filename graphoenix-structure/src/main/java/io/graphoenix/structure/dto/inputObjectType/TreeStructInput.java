package io.graphoenix.structure.dto.inputObjectType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.lang.String;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface TreeStructInput {
  String getName();

  void setName(String name);

  String getPath();

  void setPath(String path);

  Integer getDeep();

  void setDeep(Integer deep);

  String getParentId();

  void setParentId(String parentId);
}
