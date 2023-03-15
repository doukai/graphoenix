package io.graphoenix.core.dto.objectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.mysql.dto.objectType.__InputValue;
import io.graphoenix.mysql.dto.objectType.__Type;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __Field {
  @Id
  private String id;

  private String name;

  private String typeName;

  private String ofTypeName;

  private __Type ofType;

  private String description;

  @NonNull
  private Collection<__InputValue> args;

  @NonNull
  private __Type type;

  private String deprecationReason;

  private String from;

  private String to;

  private String withType;

  private String withFrom;

  private String withTo;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(String ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public __Type getOfType() {
    return this.ofType;
  }

  public void setOfType(__Type ofType) {
    this.ofType = ofType;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<__InputValue> getArgs() {
    return this.args;
  }

  public void setArgs(Collection<__InputValue> args) {
    this.args = args;
  }

  public __Type getType() {
    return this.type;
  }

  public void setType(__Type type) {
    this.type = type;
  }

  public String getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(String deprecationReason) {
    this.deprecationReason = deprecationReason;
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

  public String getWithType() {
    return this.withType;
  }

  public void setWithType(String withType) {
    this.withType = withType;
  }

  public String getWithFrom() {
    return this.withFrom;
  }

  public void setWithFrom(String withFrom) {
    this.withFrom = withFrom;
  }

  public String getWithTo() {
    return this.withTo;
  }

  public void setWithTo(String withTo) {
    this.withTo = withTo;
  }
}
