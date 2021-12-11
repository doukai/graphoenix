package io.graphoenix.spi.config;

public class NettyConfig {

    private boolean epoll;

    public NettyConfig() {
    }

    public NettyConfig(boolean epoll) {
        this.epoll = epoll;
    }

    public boolean isEpoll() {
        return epoll;
    }

    public void setEpoll(boolean epoll) {
        this.epoll = epoll;
    }
}
