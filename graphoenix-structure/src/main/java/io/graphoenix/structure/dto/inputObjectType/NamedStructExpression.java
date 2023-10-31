package io.graphoenix.structure.dto.inputObjectType;

import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface NamedStructExpression {
  StringExpression getName();

  void setName(StringExpression name);

  StringExpression getDescription();

  void setDescription(StringExpression description);
}
