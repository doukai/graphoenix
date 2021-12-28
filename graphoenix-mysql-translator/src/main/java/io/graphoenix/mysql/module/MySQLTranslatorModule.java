package io.graphoenix.mysql.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.DocumentManagerModule;
import io.graphoenix.mysql.common.utils.DBNameUtil;
import io.graphoenix.mysql.common.utils.DBValueUtil;
import io.graphoenix.mysql.handler.bootstrap.IntrospectionRegisterHandler;
import io.graphoenix.mysql.handler.bootstrap.MutationToSQLConvertHandler;
import io.graphoenix.mysql.handler.bootstrap.TypeDefiniteToCreateTableSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.SQLFormatHandler;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class MySQLTranslatorModule {

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
    public GraphQLTypeToTable graphQLTypeToTable(IGraphQLDocumentManager manager) {
        return new GraphQLTypeToTable(manager, dbNameUtil());
    }

    @Provides
    @Singleton
    public GraphQLArgumentsToWhere graphQLArgumentsToWhere(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        return new GraphQLArgumentsToWhere(manager, mapper, dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public GraphQLQueryToSelect graphQLQueryToSelect(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        return new GraphQLQueryToSelect(manager, mapper, graphQLArgumentsToWhere(manager, mapper), dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public GraphQLMutationToStatements graphQLMutationToStatements(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        return new GraphQLMutationToStatements(manager, mapper, graphQLQueryToSelect(manager, mapper), dbNameUtil(), dbValueUtil());
    }

    @Provides
    @Singleton
    public OperationToSQLConvertHandler operationToSQLConvertHandler(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        return new OperationToSQLConvertHandler(manager, graphQLQueryToSelect(manager, mapper), graphQLMutationToStatements(manager, mapper));
    }

    @Provides
    @Singleton
    public SQLFormatHandler sqlFormatHandler() {
        return new SQLFormatHandler();
    }

    @Provides
    @Singleton
    public IntrospectionRegisterHandler introspectionRegisterHandler(IGraphQLDocumentManager manager) {
        return new IntrospectionRegisterHandler(manager);
    }

    @Provides
    @Singleton
    public MutationToSQLConvertHandler mutationToSQLConvertHandler(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        return new MutationToSQLConvertHandler(graphQLMutationToStatements(manager, mapper));
    }

    @Provides
    @Singleton
    public TypeDefiniteToCreateTableSQLConvertHandler typeDefiniteToCreateTableSQLConvertHandler(IGraphQLDocumentManager manager) {
        return new TypeDefiniteToCreateTableSQLConvertHandler(graphQLTypeToTable(manager));
    }
}
