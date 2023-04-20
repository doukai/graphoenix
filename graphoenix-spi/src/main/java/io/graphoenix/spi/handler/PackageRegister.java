package io.graphoenix.spi.handler;

import io.graphoenix.spi.dto.PackageURL;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public interface PackageRegister {
    String LOAD_BALANCE_ROUND_ROBIN = "roundRobin";
    String LOAD_BALANCE_RANDOM = "random";

    default PackageURL getURL(String packageName, String protocol) {
        switch (getLoadBalance()) {
            case LOAD_BALANCE_ROUND_ROBIN:
                return getProtocolURLIterator(packageName, protocol).next();
            case LOAD_BALANCE_RANDOM:
                List<PackageURL> urlList = getProtocolURLList(packageName, protocol);
                if (urlList.size() == 1) {
                    return urlList.get(0);
                }
                int randomIndex = new Random().nextInt(urlList.size());
                return urlList.get(randomIndex);
            default:
                return getProtocolURLList(packageName, protocol).get(0);
        }
    }

    String getLoadBalance();

    List<PackageURL> getProtocolURLList(String packageName, String protocol);

    Iterator<PackageURL> getProtocolURLIterator(String packageName, String protocol);

    default PackageURL createURL(Map<String, Object> map) {
        return new PackageURL(map);
    }

    default PackageURL createURL(String protocol, String host, int port) {
        return createURL(protocol, host, port, null);
    }

    default PackageURL createURL(String protocol, String host, int port, String file) {
        return new PackageURL(protocol, host, port, file);
    }
}
