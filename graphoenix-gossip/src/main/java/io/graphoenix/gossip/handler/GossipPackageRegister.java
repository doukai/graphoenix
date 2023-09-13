package io.graphoenix.gossip.handler;

import com.google.common.collect.Iterators;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.dto.PackageURL;
import io.graphoenix.spi.handler.PackageRegister;
import io.scalecube.net.Address;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.tinylog.Logger;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("gossip")
public class GossipPackageRegister implements PackageRegister {

    private final GraphQLConfig graphQLConfig;

    private final Map<String, Set<PackageURL>> packageURLs = new ConcurrentHashMap<>();

    private final Map<String, Set<PackageURL>> memberAddressURLs = new ConcurrentHashMap<>();

    private final Map<String, Map<String, List<PackageURL>>> packageProtocolURLListMap = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Iterator<PackageURL>>> packageProtocolURLIteratorMap = new ConcurrentHashMap<>();

    @Inject
    public GossipPackageRegister(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    @Override
    public String getLoadBalance() {
        return graphQLConfig.getPackageLoadBalance();
    }

    @Override
    public List<PackageURL> getProtocolURLList(String packageName, String protocol) {
        return packageProtocolURLListMap.get(packageName).get(protocol);
    }

    @Override
    public Iterator<PackageURL> getProtocolURLIterator(String packageName, String protocol) {
        return packageProtocolURLIteratorMap.get(packageName).get(protocol);
    }

    public void mergeMemberURLs(Address address, String packageName, Map<String, Object> map) {
        PackageURL url = createURL(map);
        if (url.getHost() == null) {
            url.setHost(address.host());
        }
        packageURLs.computeIfAbsent(packageName, k -> new LinkedHashSet<>());
        packageURLs.get(packageName).add(url);
        memberAddressURLs.computeIfAbsent(address.toString(), k -> new LinkedHashSet<>());
        memberAddressURLs.get(address.toString()).add(url);
        Logger.info("package " + packageName + " service: " + url + " registered from " + address);
    }

    public void mergeMemberProtocolURLIterator(String packageName) {
        Map<String, Iterator<PackageURL>> iteratorMap = packageURLs.get(packageName).stream()
                .map(url -> new AbstractMap.SimpleEntry<>(url.getProtocol(), url))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, PackageURL>::getKey,
                                Collectors.mapping(Map.Entry<String, PackageURL>::getValue, Collectors.toList())
                        )
                )
                .entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), Iterators.cycle(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        packageProtocolURLIteratorMap.put(packageName, iteratorMap);
    }

    public void mergeMemberProtocolURLList(String packageName) {
        Map<String, List<PackageURL>> listMap = packageURLs.get(packageName).stream()
                .map(url -> new AbstractMap.SimpleEntry<>(url.getProtocol(), url))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, PackageURL>::getKey,
                                Collectors.mapping(Map.Entry<String, PackageURL>::getValue, Collectors.toList())
                        )
                );
        packageProtocolURLListMap.put(packageName, listMap);
    }

    public void removeMemberURLs(String address) {
        packageURLs.forEach((key, value) -> {
                    Set<PackageURL> urls = memberAddressURLs.get(address);
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
