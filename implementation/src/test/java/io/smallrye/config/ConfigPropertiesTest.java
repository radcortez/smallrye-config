package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConfigPropertiesTest {
    @Test
    void configProperties() {
        final SmallRyeConfig config = buildConfig("server.host", "localhost", "server.port", "8080");
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.getHost());
        assertEquals(8080, configProperties.getPort());
    }

    @Test
    void configPropertiesInterface() {
        final SmallRyeConfig config = buildConfig("server.host", "localhost", "server.port", "8080");
        final ConfigsInterface configProperties = config.getConfigProperties(ConfigsInterface.class, "server");
        assertEquals("localhost", configProperties.getHost());
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.getPort());
        assertEquals(8080, configProperties.port());
    }

    private static class Configs {
        public String host;
        public int port;

        public Configs() {
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    private interface ConfigsInterface {
        String getHost();

        Integer getPort();

        String host();

        Integer port();
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
