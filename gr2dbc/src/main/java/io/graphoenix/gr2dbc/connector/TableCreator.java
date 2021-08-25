package io.graphoenix.gr2dbc.connector;

import org.mariadb.r2dbc.MariadbConnectionConfiguration;

public class TableCreator {

    private final ConnectionCreator connectionCreator;

    public TableCreator(ConnectionCreator connectionCreator) {
        MariadbConnectionConfiguration connectionConfiguration;
        this.connectionCreator = connectionCreator;
    }

    public void createTable(String sql) {
        connectionCreator.createConnection().subscribe(connection -> {
            connection.createStatement(sql).execute();
        }, Throwable::printStackTrace);
    }
}
