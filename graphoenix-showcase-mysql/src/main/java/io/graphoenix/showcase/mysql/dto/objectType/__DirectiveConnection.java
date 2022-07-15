package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.util.Collection;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class __DirectiveConnection {
  private Integer totalCount;

  @NonNull
  @JsonAttribute(
      nullable = false
  )
  private PageInfo pageInfo;

  private Collection<__DirectiveEdge> edges;

  public Integer getTotalCount() {
    return this.totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }

  public PageInfo getPageInfo() {
    return this.pageInfo;
  }

  public void setPageInfo(PageInfo pageInfo) {
    this.pageInfo = pageInfo;
  }

  public Collection<__DirectiveEdge> getEdges() {
    return this.edges;
  }

  public void setEdges(Collection<__DirectiveEdge> edges) {
    this.edges = edges;
  }
}
