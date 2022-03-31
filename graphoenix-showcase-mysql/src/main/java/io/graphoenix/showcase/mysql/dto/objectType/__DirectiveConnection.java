package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __DirectiveConnection {
  @NonNull
  private PageInfo pageInfo;

  private Collection<__DirectiveEdge> edges;

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
