import io.graphoenix.gr2dbc.config.ConnectionConfiguration;
import io.graphoenix.gr2dbc.connector.PoolConnectionCreator;
import io.graphoenix.gr2dbc.connector.TableCreator;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.Test;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;

public class DbTest {

    @Test
    void createTable() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        MariadbConnectionConfiguration mariadbConnectionConfiguration = MariadbConnectionConfiguration.builder()
                .host(connectionConfiguration.getHost())
                .port(connectionConfiguration.getPort())
                .username(connectionConfiguration.getUsername())
                .password(connectionConfiguration.getPassword())
                .database(connectionConfiguration.getDatabase())
                .build();

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration
                .builder(new MariadbConnectionFactory(mariadbConnectionConfiguration))
                .maxIdleTime(Duration.ofMillis(connectionConfiguration.getMaxIdleTime()))
                .maxSize(connectionConfiguration.getMaxSize())
                .build();

        TableCreator tableCreator = new TableCreator(new PoolConnectionCreator(new ConnectionPool(poolConfiguration)));

        tableCreator.createTable("CREATE TABLE MyGuests (\n" +
                "id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,\n" +
                "firstname VARCHAR(30) NOT NULL,\n" +
                "lastname VARCHAR(30) NOT NULL,\n" +
                "email VARCHAR(50),\n" +
                "reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n" +
                ")").block();


    }
}
