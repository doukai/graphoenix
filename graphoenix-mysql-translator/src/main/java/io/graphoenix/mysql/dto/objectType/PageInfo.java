package io.graphoenix.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.String;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class PageInfo {
  @NonNull
  private Boolean hasNextPage;

  @NonNull
  private Boolean hasPreviousPage;

  @NonNull
  private String startCursor;

  @NonNull
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
