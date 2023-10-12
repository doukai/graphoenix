package io.graphoenix.core.dto.inputObjectType;

import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.Boolean;
import org.eclipse.microprofile.graphql.Input;

@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public interface MetaExpression {
  IntExpression getRealmId();

  void setRealmId(IntExpression realmId);

  StringExpression getCreateUserId();

  void setCreateUserId(StringExpression createUserId);

  StringExpression getCreateTime();

  void setCreateTime(StringExpression createTime);

  StringExpression getUpdateUserId();

  void setUpdateUserId(StringExpression updateUserId);

  StringExpression getUpdateTime();

  void setUpdateTime(StringExpression updateTime);

  StringExpression getCreateGroupId();

  void setCreateGroupId(StringExpression createGroupId);

  Boolean getIncludeDeprecated();

  void setIncludeDeprecated(Boolean includeDeprecated);

  IntExpression getVersion();

  void setVersion(IntExpression version);
}
