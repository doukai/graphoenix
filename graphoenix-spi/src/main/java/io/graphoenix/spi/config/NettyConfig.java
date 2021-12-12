package io.graphoenix.spi.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "netty")
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
