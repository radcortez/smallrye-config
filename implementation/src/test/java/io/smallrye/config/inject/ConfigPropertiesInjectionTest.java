package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class ConfigPropertiesInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConfigProducer.class, Configs.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    Configs configs;

    @Test
    void configProperties() {
        assertEquals("localhost", configs.getHost());
        assertEquals(8080, configs.getPort());
    }

    @ConfigProperties(prefix = "server")
    public static class Configs {
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
}
