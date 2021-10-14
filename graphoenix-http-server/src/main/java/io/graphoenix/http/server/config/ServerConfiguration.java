package io.graphoenix.http.server.config;

public class ServerConfiguration {

    private String graphqlContextPath = "graphql";
    private boolean ssl;
    private int port = 8080;

    public String getGraphqlContextPath() {
        return graphqlContextPath;
    }

    public ServerConfiguration setGraphqlContextPath(String graphqlContextPath) {
        this.graphqlContextPath = graphqlContextPath;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public ServerConfiguration setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ServerConfiguration setPort(int port) {
        this.port = port;
        return this;
    }
}
