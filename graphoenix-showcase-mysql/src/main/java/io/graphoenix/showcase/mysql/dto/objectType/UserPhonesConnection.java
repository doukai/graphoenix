package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.util.Collection;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class UserPhonesConnection {
  private Integer totalCount;

  @NonNull
  private PageInfo pageInfo;

  private Collection<UserPhonesEdge> edges;

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

  public Collection<UserPhonesEdge> getEdges() {
    return this.edges;
  }

  public void setEdges(Collection<UserPhonesEdge> edges) {
    this.edges = edges;
  }
}
