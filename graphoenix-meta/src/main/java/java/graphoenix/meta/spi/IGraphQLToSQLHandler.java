package java.graphoenix.meta.spi;

import java.util.List;

public interface IGraphQLToSQLHandler {

    String queryOperationToSelectSQL(String queryOperationGraphQL);

    List<String> queryOperationToSelectSQLList(String queryOperationGraphQL);

    List<String> mutationOperationToMergeSQLList(String mutationOperationGraphQL);

    String typeDefinitionToCreateTableSql(String typeDefinitionGraphQL);

    List<String> typeDefinitionToAlterTableSql(String typeDefinitionGraphQL);
}
