package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.spi.annotation.Skip;
import jakarta.annotation.Generated;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Skip
public class RoleRoleTypeEdge {
  private RoleRoleType node;

  @Id
  private String cursor;

  public RoleRoleType getNode() {
    return this.node;
  }

  public void setNode(RoleRoleType node) {
    this.node = node;
  }

  public String getCursor() {
    return this.cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }
}
