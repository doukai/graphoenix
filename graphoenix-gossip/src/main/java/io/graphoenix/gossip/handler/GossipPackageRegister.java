package io.graphoenix.gossip.handler;

import io.graphoenix.spi.handler.PackageRegister;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GossipPackageRegister implements PackageRegister {

    private final Map<String, Map<String, Set<String>>> members = new ConcurrentHashMap<>();

    @Override
    public URI getURI(String packageName, String protocol) {
        return null;
    }

    @Override
    public List<URI> getURIList(String packageName, String protocol) {
        return null;
    }

    @Override
    public Stream<URI> getURIStream(String packageName, String protocol) {
        return null;
    }
}
