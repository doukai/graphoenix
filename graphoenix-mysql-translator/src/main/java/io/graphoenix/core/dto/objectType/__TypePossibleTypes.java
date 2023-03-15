package io.graphoenix.core.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __TypePossibleTypes {
  @Id
  private String id;

  @NonNull
  private String typeName;

  @NonNull
  private String possibleTypeName;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getPossibleTypeName() {
    return this.possibleTypeName;
  }

  public void setPossibleTypeName(String possibleTypeName) {
    this.possibleTypeName = possibleTypeName;
  }
}
