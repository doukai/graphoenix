package io.graphoenix.http.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "http")
public class HttpServerConfig {

    @Optional
    private String graphqlContextPath = "graphql";

    @Optional
    private String subscriptionsContextPath = "subscriptions";

    @Optional
    private String schemaContextPath = "schema";

    @Optional
    private Boolean ssl = false;

    @Optional
    private Boolean tcpNoDelay = true;

    @Optional
    private Boolean soKeepAlive = false;

    @Optional
    private Integer soBackLog = 128;

    @Optional
    private Integer port = 8080;

    public String getGraphqlContextPath() {
        if (graphqlContextPath.startsWith("/")) {
            return graphqlContextPath;
        } else {
            return "/" + graphqlContextPath;
        }
    }

    public void setGraphqlContextPath(String graphqlContextPath) {
        this.graphqlContextPath = graphqlContextPath;
    }

    public String getSubscriptionsContextPath() {
        if (subscriptionsContextPath.startsWith("/")) {
            return subscriptionsContextPath;
        } else {
            return "/" + subscriptionsContextPath;
        }
    }

    public void setSubscriptionsContextPath(String subscriptionsContextPath) {
        this.subscriptionsContextPath = subscriptionsContextPath;
    }

    public String getSchemaContextPath() {
        if (schemaContextPath.startsWith("/")) {
            return schemaContextPath;
        } else {
            return "/" + schemaContextPath;
        }
    }

    public void setSchemaContextPath(String schemaContextPath) {
        this.schemaContextPath = schemaContextPath;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    public Boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(Boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public Boolean getSoKeepAlive() {
        return soKeepAlive;
    }

    public void setSoKeepAlive(Boolean soKeepAlive) {
        this.soKeepAlive = soKeepAlive;
    }

    public Integer getSoBackLog() {
        return soBackLog;
    }

    public void setSoBackLog(Integer soBackLog) {
        this.soBackLog = soBackLog;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
