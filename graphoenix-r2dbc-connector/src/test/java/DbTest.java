import io.graphoenix.common.config.YamlConfigLoader;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.handler.ReactiveSQLHandler;
import org.junit.jupiter.api.Test;

public class DbTest {

    @Test
    void createTable() {

        ConnectionConfiguration connectionConfiguration = YamlConfigLoader.YAML_CONFIG_LOADER.loadAs("beans.yaml", ConnectionConfiguration.class);
        ReactiveSQLHandler graphQLOperationHandler = new ReactiveSQLHandler(connectionConfiguration);

        String a = "";
//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("beans.yaml");
//        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);
//
//        TableCreator tableCreator = new TableCreator(new ConnectionCreator(connectionConfiguration));
//
//        tableCreator.createTable("CREATE TABLE MyGuests (\n" +
//                "id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,\n" +
//                "firstname VARCHAR(30) NOT NULL,\n" +
//                "lastname VARCHAR(30) NOT NULL,\n" +
//                "email VARCHAR(50),\n" +
//                "reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n" +
//                ")").block();


    }
}
