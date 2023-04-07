package io.graphoenix.core.handler;

import com.google.common.collect.Iterators;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.spi.handler.PackageRegister;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import java.net.URL;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Default
public class ConfigPackageRegister implements PackageRegister {

    private final GraphQLConfig graphQLConfig;

    private final Map<String, Map<String, List<URL>>> packageProtocolURLListMap;

    private final Map<String, Map<String, Iterator<URL>>> packageProtocolURLIteratorMap;

    @SuppressWarnings("unchecked")
    @Inject
    public ConfigPackageRegister(GraphQLConfig graphQLConfig, PackageConfig packageConfig) {
        this.graphQLConfig = graphQLConfig;
        this.packageProtocolURLListMap = Stream.ofNullable(packageConfig.getMembers())
                .flatMap(packageMembers -> packageMembers.entrySet().stream())
                .flatMap(packageEntry ->
                        ((List<Map<String, Object>>) packageEntry.getValue()).stream()
                                .map(this::createURL)
                                .map(url -> new AbstractMap.SimpleEntry<>(packageEntry.getKey(), new AbstractMap.SimpleEntry<>(url.getProtocol(), url)))
                )
                .collect(Collectors.groupingBy(
                        Map.Entry<String, AbstractMap.SimpleEntry<String, URL>>::getKey,
                        Collectors.mapping(
                                Map.Entry<String, AbstractMap.SimpleEntry<String, URL>>::getValue,
                                Collectors.groupingBy(
                                        Map.Entry<String, URL>::getKey,
                                        Collectors.mapping(
                                                Map.Entry<String, URL>::getValue,
                                                Collectors.toList()
                                        )
                                )
                        )
                        )
                );
        if (graphQLConfig.getPackageLoadBalance().equals(LOAD_BALANCE_ROUND_ROBIN)) {
            this.packageProtocolURLIteratorMap = Stream.ofNullable(packageConfig.getMembers())
                    .flatMap(packageMembers -> packageMembers.entrySet().stream())
                    .flatMap(packageEntry ->
                            ((List<Map<String, Object>>) packageEntry.getValue()).stream()
                                    .map(this::createURL)
                                    .map(url -> new AbstractMap.SimpleEntry<>(url.getProtocol(), url))
                                    .collect(
                                            Collectors.groupingBy(
                                                    Map.Entry<String, URL>::getKey,
                                                    Collectors.mapping(
                                                            Map.Entry<String, URL>::getValue,
                                                            Collectors.toList()
                                                    )
                                            )
                                    )
                                    .entrySet().stream()
                                    .map(protocolEntry -> new AbstractMap.SimpleEntry<>(packageEntry.getKey(), new AbstractMap.SimpleEntry<>(protocolEntry.getKey(), Iterators.cycle(protocolEntry.getValue()))))
                    )
                    .collect(Collectors.groupingBy(
                            Map.Entry<String, AbstractMap.SimpleEntry<String, Iterator<URL>>>::getKey,
                            Collectors.mapping(
                                    Map.Entry<String, AbstractMap.SimpleEntry<String, Iterator<URL>>>::getValue,
                                    Collectors.toMap(
                                            Map.Entry<String, Iterator<URL>>::getKey,
                                            Map.Entry<String, Iterator<URL>>::getValue
                                    )
                            )
                            )
                    );
        } else {
            this.packageProtocolURLIteratorMap = null;
        }
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
        return packageProtocolURLIteratorMap.get(packageName).get(packageName);
    }
}
