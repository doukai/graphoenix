package io.graphoenix.mysql.module;

import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.graphoenix.mysql.utils.DBNameUtil;
import io.graphoenix.mysql.utils.DBValueUtil;
import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.SQLFormatHandler;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class MySQLTranslatorModule {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;

    @Inject
    public MySQLTranslatorModule(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        this.manager = manager;
        this.mapper = mapper;
    }

    @Provides
    @Singleton
    public DBNameUtil dbNameUtil() {
        return new DBNameUtil();
    }

    @Provides
    @Singleton
    public DBValueUtil dbValueUtil() {
        return new DBValueUtil(dbNameUtil());
    }

    @Provides
    @Singleton
    public GraphQLTypeToTable graphQLTypeToTable() {
        return new GraphQLTypeToTable(manager, dbNameUtil());
    }

    @Provides
    @Singleton
    public GraphQLArgumentsToWhere graphQLArgumentsToWhere() {
        return new GraphQLArgumentsToWhere(manager, mapper, dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public GraphQLQueryToSelect graphQLQueryToSelect() {
        return new GraphQLQueryToSelect(manager, mapper, graphQLArgumentsToWhere(), dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public GraphQLMutationToStatements graphQLMutationToStatements() {
        return new GraphQLMutationToStatements(manager, mapper, graphQLQueryToSelect(), dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public OperationToSQLConvertHandler operationToSQLConvertHandler() {
        return new OperationToSQLConvertHandler(manager, graphQLQueryToSelect(), graphQLMutationToStatements());
    }

    @Provides
    @Singleton
    public SQLFormatHandler sqlFormatHandler() {
        return new SQLFormatHandler();
    }
}
