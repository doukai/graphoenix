package io.graphoenix.spi.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public interface PackageRegister {
    String PROTOCOL_NAME = "protocol";
    String HOST_NAME = "host";
    String PORT_NAME = "port";
    String FILE_NAME = "file";
    String LOAD_BALANCE_ROUND_ROBIN = "roundRobin";
    String LOAD_BALANCE_RANDOM = "random";

    default URL getURL(String packageName, String protocol) {
        switch (getLoadBalance()) {
            case LOAD_BALANCE_ROUND_ROBIN:
                return getProtocolURLIterator(packageName, protocol).next();
            case LOAD_BALANCE_RANDOM:
                List<URL> urlList = getProtocolURLList(packageName, protocol);
                if (urlList.size() == 1) {
                    return urlList.get(0);
                }
                int randomIndex = new Random().nextInt(urlList.size());
                return urlList.get(randomIndex);
        }
        return getProtocolURLList(packageName, protocol).get(0);
    }

    String getLoadBalance();

    List<URL> getProtocolURLList(String packageName, String protocol);

    Iterator<URL> getProtocolURLIterator(String packageName, String protocol);

    default URL createURL(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    default URL createURL(Map<String, Object> map) {
        String protocol = (String) map.get(PROTOCOL_NAME);
        String host = (String) map.get(HOST_NAME);
        int port = (int) map.getOrDefault(PORT_NAME, -1);
        String file = (String) map.getOrDefault(FILE_NAME, "");
        return createURL(protocol, host, port, file);
    }

    default URL createURL(String protocol, String host, int port) {
        return createURL(protocol, host, port, "");
    }

    default URL createURL(String protocol, String host, int port, String file) {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
