package io.graphoenix.gossip.handler;

import io.graphoenix.spi.handler.PackageRegister;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GossipPackageRegister implements PackageRegister {

    private final Map<String, Set<URL>> members = new ConcurrentHashMap<>();

    @Override
    public URL getURL(String packageName, String protocol) {
        return getURLStream(packageName, protocol).findAny().orElse(null);
    }

    @Override
    public List<URL> getURLList(String packageName, String protocol) {
        return getURLStream(packageName, protocol).collect(Collectors.toList());
    }

    @Override
    public Stream<URL> getURLStream(String packageName, String protocol) {
        return members.get(packageName).stream().filter(url -> url.getProtocol().equals(protocol));
    }

    public void mergeMembers(String packageName, String spec) {
        members.computeIfAbsent(packageName, k -> new LinkedHashSet<>());
        members.get(packageName).add(createURL(spec));
    }

    public void removeMember(String host) {
        members.entrySet()
                .forEach(entry ->
                        entry.getValue().removeAll(
                                entry.getValue().stream()
                                        .filter(url -> url.getHost().equals(host))
                                        .collect(Collectors.toSet())
                        )
                );
    }
}
