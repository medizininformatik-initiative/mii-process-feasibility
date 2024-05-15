package de.medizininformatik_initiative.process.feasibility;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.medizininformatik_initiative.process.feasibility.spring.config.NetworkStoreSettings;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FeasibilityProcessPluginDefinitionTest {

    final static Logger logger = LoggerFactory.getLogger(FeasibilityProcessPluginDefinitionTest.class);

    @Test
    public void testGetFhirResourcesByProcessId() throws Exception {
        String foo = """
                - id: foo
                  evaluationStrategy: cql
                  store:
                    timeout: 1000
                    proxy:
                      host: proxy.foo.bar
                      port: 1234
                    auth:
                      bearerToken: eydasdada
                - id: bar
                  evaluationStrategy: structured-query
                  store:
                    timeout: 999
                    proxy:
                      host: proxy.bar.foo
                      port: 4321
                    auth:
                      basic:
                        username: bar
                        password: foo
                """;
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        List<NetworkStoreSettings> values = objectMapper.readerForListOf(NetworkStoreSettings.class).readValue(foo);
    }

}
