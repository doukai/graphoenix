package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.spi.config.R2DBCConfig;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class MutationSQLExecuteHandler implements IBootstrapHandler {

    private static final Logger log = LoggerFactory.getLogger(MutationSQLExecuteHandler.class);
    private static final int sqlCount = 500;

    @Override
    @SuppressWarnings("unchecked")
    public Void transform(IGraphQLDocumentManager manager, Object sqlStream) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, R2DBCConfig.class));
        MutationExecutor mutationExecutor = new MutationExecutor(connectionCreator);
        log.info("introspection data SQL insert started");
        mutationExecutor.executeMutationsInBatchByGroup((Stream<String>) sqlStream, sqlCount)
                .forEach(count -> {
                    log.info(count + " introspection data SQL insert success");
                });
        log.info("All introspection data SQL insert success");
        return null;
    }
}