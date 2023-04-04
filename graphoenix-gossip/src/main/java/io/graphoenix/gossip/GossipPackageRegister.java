package io.graphoenix.gossip;

import io.graphoenix.spi.handler.PackageRegister;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public class GossipPackageRegister implements PackageRegister {

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
