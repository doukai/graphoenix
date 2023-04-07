package io.graphoenix.gossip.config;

import com.typesafe.config.Optional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GossipConfig {

    @Optional
    private Set<String> seedMembers;

    @Optional
    private List<Map<String, Object>> services;

    @Optional
    private Integer port;

    public Set<String> getSeedMembers() {
        return seedMembers;
    }

    public void setSeedMembers(Set<String> seedMembers) {
        this.seedMembers = seedMembers;
    }

    public List<Map<String, Object>> getServices() {
        return services;
    }

    public void setServices(List<Map<String, Object>> services) {
        this.services = services;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
