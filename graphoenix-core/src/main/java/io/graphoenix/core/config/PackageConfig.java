package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.util.Map;

@ConfigProperties(prefix = "package")
public class PackageConfig {

    @Optional
    private Map<String, Object> members;

    public Map<String, Object> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Object> members) {
        this.members = members;
    }
}
