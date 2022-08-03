package io.graphoenix.grpc.server.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "grpc")
public class GrpcServerConfig {

    @Optional
    private Integer port = 50051;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
