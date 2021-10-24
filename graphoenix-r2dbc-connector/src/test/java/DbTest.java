import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.r2dbc.connector.TableCreator;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class DbTest {

    @Test
    void createTable() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        TableCreator tableCreator = new TableCreator(new ConnectionCreator(connectionConfiguration));

        tableCreator.createTable("CREATE TABLE MyGuests (\n" +
                "id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,\n" +
                "firstname VARCHAR(30) NOT NULL,\n" +
                "lastname VARCHAR(30) NOT NULL,\n" +
                "email VARCHAR(50),\n" +
                "reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n" +
                ")").block();


    }
}
