package io.graphoenix.spi.config;

public class HttpServerConfig {

    private String graphqlContextPath = "graphql";
    private boolean ssl;
    private boolean tcpNoDelay = true;
    private boolean soKeepAlive;
    private int soBackLog = 128;
    private int port = 8080;

    public HttpServerConfig() {
    }

    public HttpServerConfig(String graphqlContextPath, boolean ssl, boolean tcpNoDelay, boolean soKeepAlive, int soBackLog, int port) {
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

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isSoKeepAlive() {
        return soKeepAlive;
    }

    public void setSoKeepAlive(boolean soKeepAlive) {
        this.soKeepAlive = soKeepAlive;
    }

    public int getSoBackLog() {
        return soBackLog;
    }

    public void setSoBackLog(int soBackLog) {
        this.soBackLog = soBackLog;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
