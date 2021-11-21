package io.graphoenix.spi.config;

public class NettyConfig {

    private boolean epoll;

    public boolean isEpoll() {
        return epoll;
    }

    public void setEpoll(boolean epoll) {
        this.epoll = epoll;
    }
}
