package io.graphoenix.showcase.order.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class MerchantPartnersEdge {
  private MerchantPartners node;

  @Id
  private String cursor;

  public MerchantPartners getNode() {
    return this.node;
  }

  public void setNode(MerchantPartners node) {
    this.node = node;
  }

  public String getCursor() {
    return this.cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }
}
