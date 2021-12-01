package io.graphoenix.showcase.mysql.generated.inputObjectType;

import java.lang.String;
import org.eclipse.microprofile.graphql.Input;

@Input
public class MapWith {
  private String type;

  private String from;

  private String to;

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFrom() {
    return this.from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return this.to;
  }

  public void setTo(String to) {
    this.to = to;
  }
}