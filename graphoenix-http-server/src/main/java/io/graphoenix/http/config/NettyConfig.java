package io.graphoenix.http.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "netty")
public class NettyConfig {

    @Optional
    private Boolean epoll = false;

    public Boolean getEpoll() {
        return epoll;
    }

    public void setEpoll(Boolean epoll) {
        this.epoll = epoll;
    }
}
