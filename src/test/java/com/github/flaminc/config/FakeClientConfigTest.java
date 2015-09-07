package com.github.flaminc.config;

import com.github.flaminc.client.FakeClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;


public class FakeClientConfigTest {
    private static final Logger log = LoggerFactory.getLogger(FakeClientConfigTest.class);

    @Test
    public void testEmptyConfig() throws Exception {
        FakeClient client = new FakeClient();

        Config config = ConfigFactory.empty();
        ConfiguratorUtil.noConfig().loadClass(config, client);

        final FakeClient expected = new FakeClient();
        expected.init();
        assertThat(client).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testNullConfig() throws Exception {
        FakeClient client = new FakeClient();
        ConfiguratorUtil.noConfig().loadClass(null, client);

        final FakeClient expected = new FakeClient();
        expected.init();
        assertThat(client).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testClientConfig() throws Exception {
        FakeClient client = new FakeClient();

        Config config = getClientConfig();

        ConfiguratorUtil.noConfig().loadClass(config, client);
        final FakeClient expected = new FakeClient();
        expected.setAccessKey("9MALAVRPG4QP1IBJ5DLV");
        expected.setSecretKey("NF2v7zNwzdEEfOVpgrYGSQRJmxXEALOzQQheKMGaIw8xDgNjdFR6Aog2YUYirFZ");
        expected.setUrl(new URL("https://www.google.com"));
        expected.setProxyHost("localhost");
        expected.setProxyPort(3128);
        expected.init();
        assertThat(client).isEqualToComparingFieldByField(expected);
    }

    public static Config getClientConfig() {
        Config config = ConfigFactory.parseResources("/client.conf");
        assertThat(config.entrySet()).isNotEmpty();
        return config;
    }
}