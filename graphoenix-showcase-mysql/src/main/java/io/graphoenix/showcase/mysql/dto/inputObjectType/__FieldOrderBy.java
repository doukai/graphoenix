package io.graphoenix.showcase.mysql.dto.inputObjectType;

import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __FieldOrderBy {
  private Sort id;

  private Sort name;

  private Sort typeName;

  private Sort ofTypeName;

  private Sort description;

  private Sort deprecationReason;

  private Sort from;

  private Sort to;

  private Sort withType;

  private Sort withFrom;

  private Sort withTo;

  private Sort version;

  private Sort isDeprecated;

  private Sort __typename;

  public Sort getId() {
    return this.id;
  }

  public void setId(Sort id) {
    this.id = id;
  }

  public Sort getName() {
    return this.name;
  }

  public void setName(Sort name) {
    this.name = name;
  }

  public Sort getTypeName() {
    return this.typeName;
  }

  public void setTypeName(Sort typeName) {
    this.typeName = typeName;
  }

  public Sort getOfTypeName() {
    return this.ofTypeName;
  }

  public void setOfTypeName(Sort ofTypeName) {
    this.ofTypeName = ofTypeName;
  }

  public Sort getDescription() {
    return this.description;
  }

  public void setDescription(Sort description) {
    this.description = description;
  }

  public Sort getDeprecationReason() {
    return this.deprecationReason;
  }

  public void setDeprecationReason(Sort deprecationReason) {
    this.deprecationReason = deprecationReason;
  }

  public Sort getFrom() {
    return this.from;
  }

  public void setFrom(Sort from) {
    this.from = from;
  }

  public Sort getTo() {
    return this.to;
  }

  public void setTo(Sort to) {
    this.to = to;
  }

  public Sort getWithType() {
    return this.withType;
  }

  public void setWithType(Sort withType) {
    this.withType = withType;
  }

  public Sort getWithFrom() {
    return this.withFrom;
  }

  public void setWithFrom(Sort withFrom) {
    this.withFrom = withFrom;
  }

  public Sort getWithTo() {
    return this.withTo;
  }

  public void setWithTo(Sort withTo) {
    this.withTo = withTo;
  }

  public Sort getVersion() {
    return this.version;
  }

  public void setVersion(Sort version) {
    this.version = version;
  }

  public Sort getIsDeprecated() {
    return this.isDeprecated;
  }

  public void setIsDeprecated(Sort isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public Sort get__Typename() {
    return this.__typename;
  }

  public void set__Typename(Sort __typename) {
    this.__typename = __typename;
  }
}
