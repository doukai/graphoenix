package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.String;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class PageInfo {
  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private Boolean hasNextPage;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private Boolean hasPreviousPage;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private String startCursor;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private String endCursor;

  public Boolean getHasNextPage() {
    return this.hasNextPage;
  }

  public void setHasNextPage(Boolean hasNextPage) {
    this.hasNextPage = hasNextPage;
  }

  public Boolean getHasPreviousPage() {
    return this.hasPreviousPage;
  }

  public void setHasPreviousPage(Boolean hasPreviousPage) {
    this.hasPreviousPage = hasPreviousPage;
  }

  public String getStartCursor() {
    return this.startCursor;
  }

  public void setStartCursor(String startCursor) {
    this.startCursor = startCursor;
  }

  public String getEndCursor() {
    return this.endCursor;
  }

  public void setEndCursor(String endCursor) {
    this.endCursor = endCursor;
  }
}
