package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;

public interface IGraphQLTypeToCrateSQLHandler extends IGraphQLTypeHandler<IGraphqlDocumentManager, SQLStatements> {

    SQLStatements convert(IGraphqlDocumentManager manager);
}
