package io.graphoenix.showcase.mysql.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class __TypeInterfacesConnection {
  private Integer totalCount;

  private PageInfo pageInfo;

  private Collection<__TypeInterfacesEdge> edges;

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

  public Collection<__TypeInterfacesEdge> getEdges() {
    return this.edges;
  }

  public void setEdges(Collection<__TypeInterfacesEdge> edges) {
    this.edges = edges;
  }
}
