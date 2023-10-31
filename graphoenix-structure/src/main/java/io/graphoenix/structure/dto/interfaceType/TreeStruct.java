package io.graphoenix.structure.dto.interfaceType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.lang.String;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.NonNull;

@Interface
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface TreeStruct {
  @NonNull
  String path = null;

  Integer deep = null;

  String getPath();

  void setPath(String path);

  Integer getDeep();

  void setDeep(Integer deep);
}
