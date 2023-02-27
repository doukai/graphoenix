package io.graphoenix.core.handler;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PackageManager {
    private final ConcurrentHashMap<String, URI> URIMap = new ConcurrentHashMap<>();

    public URI getURI(String packageName) {
        return URIMap.get(packageName);
    }
}
