package io.graphoenix.showcase.mysql.dto.objectType;

import io.graphoenix.showcase.mysql.dto.enumType.__DirectiveLocation;
import io.graphoenix.showcase.mysql.dto.interfaceType.Meta;
import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public class __DirectiveLocations implements Meta {
  @Id
  private String id;

  @NonNull
  private String directiveName;

  @NonNull
  private __DirectiveLocation directiveLocation;

  private Integer version;

  private Boolean isDeprecated;

  private String __typename;

  private Integer idCount;

  @Id
  private String idMax;

  @Id
  private String idMin;

  private Integer directiveNameCount;

  private String directiveNameMax;

  private String directiveNameMin;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(String directiveName) {
    this.directiveName = directiveName;
  }

  public __DirectiveLocation getDirectiveLocation() {
    return this.directiveLocation;
  }

  public void setDirectiveLocation(__DirectiveLocation directiveLocation) {
    this.directiveLocation = directiveLocation;
  }

  @Override
  public Integer getVersion() {
    return this.version;
  }

  @Override
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public Boolean getIsDeprecated() {
    return this.isDeprecated;
  }

  @Override
  public void setIsDeprecated(Boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public String get__Typename() {
    return this.__typename;
  }

  public void set__Typename(String __typename) {
    this.__typename = __typename;
  }

  public Integer getIdCount() {
    return this.idCount;
  }

  public void setIdCount(Integer idCount) {
    this.idCount = idCount;
  }

  public String getIdMax() {
    return this.idMax;
  }

  public void setIdMax(String idMax) {
    this.idMax = idMax;
  }

  public String getIdMin() {
    return this.idMin;
  }

  public void setIdMin(String idMin) {
    this.idMin = idMin;
  }

  public Integer getDirectiveNameCount() {
    return this.directiveNameCount;
  }

  public void setDirectiveNameCount(Integer directiveNameCount) {
    this.directiveNameCount = directiveNameCount;
  }

  public String getDirectiveNameMax() {
    return this.directiveNameMax;
  }

  public void setDirectiveNameMax(String directiveNameMax) {
    this.directiveNameMax = directiveNameMax;
  }

  public String getDirectiveNameMin() {
    return this.directiveNameMin;
  }

  public void setDirectiveNameMin(String directiveNameMin) {
    this.directiveNameMin = directiveNameMin;
  }
}
