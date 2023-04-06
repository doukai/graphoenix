package io.graphoenix.gossip.config;

import com.typesafe.config.Optional;

import java.util.Set;

public class GossipConfig {

    @Optional
    private Set<String> seedMembers;

    public Set<String> getSeedMembers() {
        return seedMembers;
    }

    public void setSeedMembers(Set<String> seedMembers) {
        this.seedMembers = seedMembers;
    }
}
