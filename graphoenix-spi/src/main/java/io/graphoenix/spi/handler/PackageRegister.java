package io.graphoenix.spi.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

public interface PackageRegister {

    URL getURL(String packageName, String protocol);

    List<URL> getURLList(String packageName, String protocol);

    Stream<URL> getURLStream(String packageName, String protocol);

    default URL createURL(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
