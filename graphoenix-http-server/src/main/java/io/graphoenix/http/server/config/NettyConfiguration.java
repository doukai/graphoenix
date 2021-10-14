package io.graphoenix.http.server.config;

public class NettyConfiguration {

    private boolean epoll;

    public boolean isEpoll() {
        return epoll;
    }

    public void setEpoll(boolean epoll) {
        this.epoll = epoll;
    }
}
