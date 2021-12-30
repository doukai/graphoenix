package io.graphoenix.product.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "mysql")
public class MysqlConfig {

    @Optional
    private Boolean crateTable = true;

    @Optional
    private Boolean crateIntrospection = false;

    public Boolean getCrateTable() {
        return crateTable;
    }

    public MysqlConfig setCrateTable(Boolean crateTable) {
        this.crateTable = crateTable;
        return this;
    }

    public Boolean getCrateIntrospection() {
        return crateIntrospection;
    }

    public MysqlConfig setCrateIntrospection(Boolean crateIntrospection) {
        this.crateIntrospection = crateIntrospection;
        return this;
    }
}
