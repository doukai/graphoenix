package io.graphoenix.gossip.handler;

import com.google.common.collect.Iterators;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.handler.PackageRegister;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URL;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class GossipPackageRegister implements PackageRegister {

    private final GraphQLConfig graphQLConfig;

    private final Map<String, Set<URL>> packageURLs = new ConcurrentHashMap<>();

    private final Map<String, Set<URL>> memberAddressURLs = new ConcurrentHashMap<>();

    private final Map<String, Map<String, List<URL>>> packageProtocolURLListMap = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Iterator<URL>>> packageProtocolURLIteratorMap = new ConcurrentHashMap<>();

    @Inject
    public GossipPackageRegister(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    @Override
    public String getLoadBalance() {
        return graphQLConfig.getPackageLoadBalance();
    }

    @Override
    public List<URL> getProtocolURLList(String packageName, String protocol) {
        return packageProtocolURLListMap.get(packageName).get(protocol);
    }

    @Override
    public Iterator<URL> getProtocolURLIterator(String packageName, String protocol) {
        return packageProtocolURLIteratorMap.get(packageName).get(protocol);
    }

    public void mergeMemberURLs(String address, String packageName, String protocol, String host, int port, String fileName) {
        URL url = createURL(protocol, host, port, fileName);
        packageURLs.computeIfAbsent(packageName, k -> new LinkedHashSet<>());
        packageURLs.get(packageName).add(url);
        memberAddressURLs.computeIfAbsent(address, k -> new LinkedHashSet<>());
        memberAddressURLs.get(address).add(url);
    }

    public void mergeMemberProtocolURLIterator(String packageName) {
        Map<String, Iterator<URL>> iteratorMap = packageURLs.get(packageName).stream()
                .map(url -> new AbstractMap.SimpleEntry<>(url.getProtocol(), url))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, URL>::getKey,
                                Collectors.mapping(Map.Entry<String, URL>::getValue, Collectors.toList())
                        )
                )
                .entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), Iterators.cycle(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        packageProtocolURLIteratorMap.put(packageName, iteratorMap);
    }

    public void mergeMemberProtocolURLList(String packageName) {
        Map<String, List<URL>> listMap = packageURLs.get(packageName).stream()
                .map(url -> new AbstractMap.SimpleEntry<>(url.getProtocol(), url))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, URL>::getKey,
                                Collectors.mapping(Map.Entry<String, URL>::getValue, Collectors.toList())
                        )
                );
        packageProtocolURLListMap.put(packageName, listMap);
    }

    public void removeMemberURLs(String address) {
        packageURLs.forEach((key, value) -> {
                    Set<URL> urls = memberAddressURLs.get(address);
                    if (urls != null && !urls.isEmpty()) {
                        boolean changed = value.removeAll(urls);
                        if (changed) {
                            mergeMemberProtocolURLList(key);
                            if (graphQLConfig.getPackageLoadBalance().equals(LOAD_BALANCE_ROUND_ROBIN)) {
                                mergeMemberProtocolURLIterator(key);
                            }
                        }
                    }
                }
        );
        memberAddressURLs.remove(address);
    }
}
