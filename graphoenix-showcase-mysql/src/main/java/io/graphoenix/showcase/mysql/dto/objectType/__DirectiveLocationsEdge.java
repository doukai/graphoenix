package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __DirectiveLocationsEdge {
  private __DirectiveLocations node;

  @Id
  private String cursor;

  public __DirectiveLocations getNode() {
    return this.node;
  }

  public void setNode(__DirectiveLocations node) {
    this.node = node;
  }

  public String getCursor() {
    return this.cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }
}
