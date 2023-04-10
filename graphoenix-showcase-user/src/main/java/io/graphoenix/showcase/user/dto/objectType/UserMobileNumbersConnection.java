package io.graphoenix.showcase.user.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Integer;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class UserMobileNumbersConnection {
  private Integer totalCount;

  private PageInfo pageInfo;

  private Collection<UserMobileNumbersEdge> edges;

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

  public Collection<UserMobileNumbersEdge> getEdges() {
    return this.edges;
  }

  public void setEdges(Collection<UserMobileNumbersEdge> edges) {
    this.edges = edges;
  }
}
