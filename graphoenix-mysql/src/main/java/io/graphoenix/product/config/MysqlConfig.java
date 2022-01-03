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

    public void setCrateTable(Boolean crateTable) {
        this.crateTable = crateTable;
    }

    public Boolean getCrateIntrospection() {
        return crateIntrospection;
    }

    public void setCrateIntrospection(Boolean crateIntrospection) {
        this.crateIntrospection = crateIntrospection;
    }
}
