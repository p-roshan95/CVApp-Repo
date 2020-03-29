package org.cv.app;

import java.util.Map;
import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class ConfigurationInitializer {

    private ConfigurationInitializer() {
    }

    public static Properties initKafkaConfig(Config config, String configSection) {
        Properties properties = new Properties();
        for (Map.Entry<String, ConfigValue> kafkaEntry : config.getConfig(configSection).entrySet()) {
            properties.put(kafkaEntry.getKey(), kafkaEntry.getValue().unwrapped());
        }
        return properties;
    }
}
