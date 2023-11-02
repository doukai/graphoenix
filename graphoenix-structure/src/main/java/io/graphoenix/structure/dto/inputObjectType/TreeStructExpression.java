package io.graphoenix.structure.dto.inputObjectType;

import io.graphoenix.core.dto.inputObjectType.IntExpression;
import io.graphoenix.core.dto.inputObjectType.StringExpression;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface TreeStructExpression {
  StringExpression getName();

  void setName(StringExpression name);

  StringExpression getPath();

  void setPath(StringExpression path);

  IntExpression getDeep();

  void setDeep(IntExpression deep);

  StringExpression getParentId();

  void setParentId(StringExpression parentId);
}
