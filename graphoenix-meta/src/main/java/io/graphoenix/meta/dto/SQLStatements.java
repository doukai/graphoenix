package io.graphoenix.meta.dto;

import java.util.List;

public class SQLStatements {

    List<String> sqlStatements;

    public SQLStatements(List<String> sqlStatements) {
        this.sqlStatements = sqlStatements;
    }

    public List<String> getSqlStatements() {
        return sqlStatements;
    }

    public void setSqlStatements(List<String> sqlStatements) {
        this.sqlStatements = sqlStatements;
    }
}
