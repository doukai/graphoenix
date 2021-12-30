package io.graphoenix.r2dbc.connector.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "r2dbc")
public class R2DBCConfig {

    @Optional
    private String driver = "mariadb";

    @Optional
    private String protocol = "pipes";

    private String database;

    @Optional
    private String host = "localhost";

    @Optional
    private Integer port = 3306;

    private String user;

    private String password;

    @Optional
    private Boolean usePool = true;

    @Optional
    private Integer poolMaxSize = 20;

    @Optional
    private Long poolMaxIdleTime = 1000L;

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
