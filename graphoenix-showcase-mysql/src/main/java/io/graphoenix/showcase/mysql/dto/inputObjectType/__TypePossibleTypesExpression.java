package io.graphoenix.showcase.mysql.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.showcase.mysql.dto.enumType.Conditional;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.util.Collection;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Input;

@Input
@CompiledJson
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class __TypePossibleTypesExpression {
  private StringExpression id;

  private StringExpression typeName;

  private StringExpression possibleTypeName;

  private StringExpression __typename;

  @DefaultValue("AND")
  private Conditional cond;

  private Collection<__TypePossibleTypesExpression> exs;

  public StringExpression getId() {
    return this.id;
  }

  public void setId(StringExpression id) {
    this.id = id;
  }

  public StringExpression getTypeName() {
    return this.typeName;
  }

  public void setTypeName(StringExpression typeName) {
    this.typeName = typeName;
  }

  public StringExpression getPossibleTypeName() {
    return this.possibleTypeName;
  }

  public void setPossibleTypeName(StringExpression possibleTypeName) {
    this.possibleTypeName = possibleTypeName;
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

  public Collection<__TypePossibleTypesExpression> getExs() {
    return this.exs;
  }

  public void setExs(Collection<__TypePossibleTypesExpression> exs) {
    this.exs = exs;
  }
}
