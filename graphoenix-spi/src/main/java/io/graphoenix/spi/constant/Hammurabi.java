package io.graphoenix.spi.constant;

public class Hammurabi {

    public static final String META_INTERFACE_NAME = "Meta";
    public static final String DEPRECATED_FIELD_NAME = "isDeprecated";
    public static final String DEPRECATED_INPUT_NAME = "includeDeprecated";
    public static final String LIST_INPUT_NAME = "list";
    public static final String WHERE_INPUT_NAME = "where";
    public static final String FIRST_INPUT_NAME = "first";
    public static final String LAST_INPUT_NAME = "last";
    public static final String OFFSET_INPUT_NAME = "offset";
    public static final String AFTER_INPUT_NAME = "after";
    public static final String BEFORE_INPUT_NAME = "before";
    public static final String ORDER_BY_INPUT_NAME = "orderBy";
    public static final String GROUP_BY_INPUT_NAME = "groupBy";
    public static final String SORT_INPUT_NAME = "sort";
    public static final String[] EXCLUDE_INPUT = {DEPRECATED_INPUT_NAME, FIRST_INPUT_NAME, LAST_INPUT_NAME, OFFSET_INPUT_NAME, AFTER_INPUT_NAME, BEFORE_INPUT_NAME, GROUP_BY_INPUT_NAME, ORDER_BY_INPUT_NAME, SORT_INPUT_NAME, LIST_INPUT_NAME, WHERE_INPUT_NAME};
    public static final String FUNC_DIRECTIVE_NAME = "func";
    public static final String CONNECTION_DIRECTIVE_NAME = "connection";
    public static final String FETCH_DIRECTIVE_NAME = "fetch";
    public static final String UPDATE_DIRECTIVE_NAME = "update";
    public static final String DELETE_DIRECTIVE_NAME = "delete";
    public static final String MERGE_TO_LIST_DIRECTIVE_NAME = "mergeToList";
    public static final String INTROSPECTION_PREFIX = "__";
    public static final String INPUT_SUFFIX = "Input";
    public static final String ORDER_BY_SUFFIX = "OrderBy";
    public static final String PAGE_INFO_NAME = "PageInfo";
    public static final String CONNECTION_SUFFIX = "Connection";
    public static final String AGGREGATE_SUFFIX = "Aggregate";
    public static final String EDGE_SUFFIX = "Edge";
    public static final String EXPRESSION_SUFFIX = "Expression";
    public static final String[] INVOKE_DIRECTIVES = {"invoke", "fetch"};
    public static final String[] CONTAINER_DIRECTIVES = {"containerType", "fetchType"};
    public static final String REQUEST_ID = "requestId";
    public static final String SESSION_ID = "sessionId";
    public static final String TRANSACTION_ID = "TransactionId";
    public static final String TRANSACTION_TYPE = "TransactionType";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String GRAPHQL_REQUEST = "graphQLRequest";
    public static final String CURRENT_USER = "currentUser";
    public static final String OPERATION_DEFINITION = "operationDefinition";
    public static final String PERMIT_ALL = "permitAll";

    public enum TransactionType {
        NO_TRANSACTION, IN_TRANSACTION
    }

    public enum MutationType {
        MERGE, UPDATE, DELETE
    }
}
