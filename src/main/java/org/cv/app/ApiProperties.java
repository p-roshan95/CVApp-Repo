package org.cv.app;

import java.util.Properties;

import com.typesafe.config.Config;

public class ApiProperties {

    private static final String API_KEY = "api";
    private static final String KAFKA_KEY = API_KEY + ".kafka";
    private static final String KAFKA_PROPERTIES_KEY = KAFKA_KEY + ".properties";
    private static final String SCHEMA_REGISTRY_URL_KEY = API_KEY + ".schema.registry.url";

    private final Properties kafkaProperties;
    private final String schemaRegistryURL;

    public ApiProperties(Config config) {
        kafkaProperties = ConfigurationInitializer.initKafkaConfig(config, KAFKA_PROPERTIES_KEY);
        schemaRegistryURL = config.getString(SCHEMA_REGISTRY_URL_KEY);
    }

    public Properties getKafkaProperties() {
        return kafkaProperties;
    }

    public String getSchemaRegistryURL() {
        return schemaRegistryURL;
    }
}