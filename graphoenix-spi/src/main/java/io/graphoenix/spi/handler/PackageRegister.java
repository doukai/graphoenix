package io.graphoenix.spi.handler;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public interface PackageRegister {

    URI getURI(String packageName, String protocol);

    List<URI> getURIList(String packageName, String protocol);

    Stream<URI> getURIStream(String packageName, String protocol);
}
