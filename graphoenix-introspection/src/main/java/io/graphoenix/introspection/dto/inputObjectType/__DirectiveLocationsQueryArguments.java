package io.graphoenix.introspection.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.core.dto.enumType.Conditional;
import io.graphoenix.core.dto.inputObjectType.IntExpression;
import io.graphoenix.core.dto.inputObjectType.MetaExpression;
import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.core.dto.inputObjectType.__DirectiveLocationExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __DirectiveLocationsQueryArguments implements MetaExpression {
  private StringExpression id;

  private StringExpression directiveName;

  private __DirectiveExpression directiveNameType;

  private __DirectiveLocationExpression directiveLocation;

  @DefaultValue("false")
  private Boolean includeDeprecated;

  private IntExpression version;

  private IntExpression realmId;

  private StringExpression createUserId;

  private StringExpression createTime;

  private StringExpression updateUserId;

  private StringExpression updateTime;

  private StringExpression createGroupId;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__DirectiveLocationsExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
    this.id = id;
  }

  public StringExpression getDirectiveName() {
    return this.directiveName;
  }

  public void setDirectiveName(StringExpression directiveName) {
    this.directiveName = directiveName;
  }

  public __DirectiveExpression getDirectiveNameType() {
    return this.directiveNameType;
  }

  public void setDirectiveNameType(__DirectiveExpression directiveNameType) {
    this.directiveNameType = directiveNameType;
  }

  public __DirectiveLocationExpression getDirectiveLocation() {
    return this.directiveLocation;
  }

  public void setDirectiveLocation(__DirectiveLocationExpression directiveLocation) {
    this.directiveLocation = directiveLocation;
  }

  public Boolean getIncludeDeprecated() {
    return this.includeDeprecated;
  }

  public void setIncludeDeprecated(Boolean includeDeprecated) {
    this.includeDeprecated = includeDeprecated;
  }

  public IntExpression getVersion() {
    return this.version;
  }

  public void setVersion(IntExpression version) {
    this.version = version;
  }

  public IntExpression getRealmId() {
    return this.realmId;
  }

  public void setRealmId(IntExpression realmId) {
    this.realmId = realmId;
  }

  public StringExpression getCreateUserId() {
    return this.createUserId;
  }

  public void setCreateUserId(StringExpression createUserId) {
    this.createUserId = createUserId;
  }

  public StringExpression getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(StringExpression createTime) {
    this.createTime = createTime;
  }

  public StringExpression getUpdateUserId() {
    return this.updateUserId;
  }

  public void setUpdateUserId(StringExpression updateUserId) {
    this.updateUserId = updateUserId;
  }

  public StringExpression getUpdateTime() {
    return this.updateTime;
  }

  public void setUpdateTime(StringExpression updateTime) {
    this.updateTime = updateTime;
  }

  public StringExpression getCreateGroupId() {
    return this.createGroupId;
  }

  public void setCreateGroupId(StringExpression createGroupId) {
    this.createGroupId = createGroupId;
  }

  public StringExpression get__typename() {
    return this.__typename;
  }

  public void set__typename(StringExpression __typename) {
    this.__typename = __typename;
  }

  public Conditional getCond() {
    return this.cond;
  }

  public void setCond(Conditional cond) {
    this.cond = cond;
  }

  public Collection<__DirectiveLocationsExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__DirectiveLocationsExpression> exs) {
    this.exs = exs;
  }
}
