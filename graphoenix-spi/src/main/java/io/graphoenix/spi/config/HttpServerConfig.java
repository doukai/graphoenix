package io.graphoenix.spi.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "http")
public class HttpServerConfig {

    private String graphqlContextPath = "graphql";
    private Boolean ssl;
    private Boolean tcpNoDelay = true;
    private Boolean soKeepAlive;
    private Integer soBackLog = 128;
    private Integer port = 8080;

    public HttpServerConfig(String graphqlContextPath, Boolean ssl, Boolean tcpNoDelay, Boolean soKeepAlive, Integer soBackLog, Integer port) {
        this.graphqlContextPath = graphqlContextPath;
        this.ssl = ssl;
        this.tcpNoDelay = tcpNoDelay;
        this.soKeepAlive = soKeepAlive;
        this.soBackLog = soBackLog;
        this.port = port;
    }

    public String getGraphqlContextPath() {
        return graphqlContextPath;
    }

    public void setGraphqlContextPath(String graphqlContextPath) {
        this.graphqlContextPath = graphqlContextPath;
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
