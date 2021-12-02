package io.graphoenix.mysql.handler.operation;

import io.graphoenix.common.manager.GraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.handler.IPipelineContext;
import org.javatuples.Pair;

import java.util.stream.Stream;

public class OperationToSQLConvertHandler implements IOperationHandler {

    private GraphQLQueryToSelect graphqlQueryToSelect;
    private GraphQLMutationToStatements graphqlMutationToStatements;
    private IGraphQLDocumentManager manager;
//    private MysqlTranslateConfig config;

    @Override
    public void init(IPipelineContext context) {
        this.manager = context.getManager();
        GraphQLFieldMapManager mapper = new GraphQLFieldMapManager(manager);
        this.graphqlQueryToSelect = new GraphQLQueryToSelect(manager, mapper, new GraphQLArgumentsToWhere(manager, mapper));
        this.graphqlMutationToStatements = new GraphQLMutationToStatements(manager, mapper, this.graphqlQueryToSelect);
//        this.config = YAML_CONFIG_LOADER.loadAs(Hammurabi.configName, MysqlTranslateConfig.class);
    }

    @Override
    public void query(IPipelineContext context) throws Exception {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        String sql = this.graphqlQueryToSelect.createSelectSQL(graphQL);
        context.add(sql);
    }

    @Override
    public void queryAsync(IPipelineContext context) throws Exception {
        query(context);
    }

    @Override
    public void querySelectionsAsync(IPipelineContext context) throws Exception {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        Stream<Pair<String, String>> sqlPair = this.graphqlQueryToSelect.createSelectsSQL(graphQL);
        context.add(sqlPair);
    }

    @Override
    public void mutation(IPipelineContext context) throws Exception {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        Stream<String> sqlStream = this.graphqlMutationToStatements.createStatementsSQL(graphQL);
        context.add(sqlStream);
    }

    @Override
    public void mutationAsync(IPipelineContext context) throws Exception {
        mutation(context);
    }

    @Override
    public void subscription(IPipelineContext context) throws Exception {
        //TODO
    }
}
