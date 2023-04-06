package io.graphoenix.core.handler;

import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.spi.handler.PackageRegister;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ConfigPackageRegister implements PackageRegister {

    private final PackageConfig packageConfig;

    @Inject
    public ConfigPackageRegister(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public URL getURL(String packageName, String protocol) {
        return getURLStream(packageName, protocol).findAny().orElse(null);
    }

    @Override
    public List<URL> getURLList(String packageName, String protocol) {
        return getURLStream(packageName, protocol).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<URL> getURLStream(String packageName, String protocol) {
        return Stream.ofNullable(packageConfig.getMembers())
                .flatMap(members -> Stream.ofNullable(members.get(packageName)))
                .map(packageMembers -> (List<String>) packageMembers)
                .flatMap(packageMembers -> Stream.ofNullable(packageMembers).flatMap(Collection::stream))
                .map(this::createURL);
    }
}
