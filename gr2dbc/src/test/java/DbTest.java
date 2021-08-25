import io.graphoenix.gr2dbc.config.DbConfig;
import org.junit.jupiter.api.Test;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class DbTest {

    @Test
    void createTable() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        DbConfig dbConfig = yaml.load(inputStream);

        MariadbConnectionConfiguration conf = MariadbConnectionConfiguration.builder()
                .host(dbConfig.getHost())
                .port(dbConfig.getPort())
                .username(dbConfig.getUsername())
                .password(dbConfig.getPassword())
                .database(dbConfig.getDatabase())
                .build();

        // Instantiate a Connection Factory
        new MariadbConnectionFactory(conf);
    }
}
