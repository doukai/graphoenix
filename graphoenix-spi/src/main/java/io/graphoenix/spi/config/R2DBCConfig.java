package io.graphoenix.spi.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "r2dbc")
public class R2DBCConfig {
    private String driver;
    private String protocol = "pipes";
    private String database;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private Boolean usePool;
    private Integer poolMaxSize;
    private Long poolMaxIdleTime;

    public R2DBCConfig(String driver, String protocol, String database, String host, Integer port, String user, String password, Boolean usePool, Integer poolMaxSize, Long poolMaxIdleTime) {
        this.driver = driver;
        this.protocol = protocol;
        this.database = database;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.usePool = usePool;
        this.poolMaxSize = poolMaxSize;
        this.poolMaxIdleTime = poolMaxIdleTime;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getUsePool() {
        return usePool;
    }

    public void setUsePool(Boolean usePool) {
        this.usePool = usePool;
    }

    public Integer getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(Integer poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public Long getPoolMaxIdleTime() {
        return poolMaxIdleTime;
    }

    public void setPoolMaxIdleTime(Long poolMaxIdleTime) {
        this.poolMaxIdleTime = poolMaxIdleTime;
    }
}
