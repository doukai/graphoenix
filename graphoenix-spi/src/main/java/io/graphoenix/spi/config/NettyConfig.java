package io.graphoenix.spi.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "netty")
public class NettyConfig {

    private Boolean epoll;

    public NettyConfig(Boolean epoll) {
        this.epoll = epoll;
    }

    public Boolean getEpoll() {
        return epoll;
    }

    public void setEpoll(Boolean epoll) {
        this.epoll = epoll;
    }
}
