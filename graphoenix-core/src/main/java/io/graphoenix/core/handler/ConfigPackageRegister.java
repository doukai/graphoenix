package io.graphoenix.core.handler;

import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.spi.handler.PackageRegister;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    public URI getURI(String packageName, String protocol) {
        return getURIStream(packageName, protocol).findAny().orElse(null);
    }

    @Override
    public List<URI> getURIList(String packageName, String protocol) {
        return getURIStream(packageName, protocol).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<URI> getURIStream(String packageName, String protocol) {
        return Stream.ofNullable(packageConfig.getMembers())
                .flatMap(members -> Stream.ofNullable(members.get(packageName)))
                .map(packageMember -> (Map<String, List<String>>) packageMember)
                .flatMap(packageMember -> Stream.ofNullable(packageMember.get(protocol)))
                .flatMap(Collection::stream)
                .map(URI::create);
    }
}
