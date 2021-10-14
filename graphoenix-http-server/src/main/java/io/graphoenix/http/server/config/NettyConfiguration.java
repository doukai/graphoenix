package io.graphoenix.http.server.config;

public class NettyConfiguration {

    private boolean ssl;
    private boolean epoll;
    private boolean tcpNoDelay = true;
    private boolean soKeepAlive;
    private int soBackLog = 128;

    public boolean isSsl() {
        return ssl;
    }

    public NettyConfiguration setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public boolean isEpoll() {
        return epoll;
    }

    public NettyConfiguration setEpoll(boolean epoll) {
        this.epoll = epoll;
        return this;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public NettyConfiguration setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public boolean isSoKeepAlive() {
        return soKeepAlive;
    }

    public NettyConfiguration setSoKeepAlive(boolean soKeepAlive) {
        this.soKeepAlive = soKeepAlive;
        return this;
    }

    public int getSoBackLog() {
        return soBackLog;
    }

    public NettyConfiguration setSoBackLog(int soBackLog) {
        this.soBackLog = soBackLog;
        return this;
    }
}
