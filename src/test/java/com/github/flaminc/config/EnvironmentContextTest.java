package com.github.flaminc.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class EnvironmentContextTest {

    @After
    public void after() throws Exception {
        // restore no environment
        getEnvironment().remove(EnvironmentContext.SYSTEM_ENVIRONMENT);
    }

    @Test
    public void testGetEnvironmentException() throws Exception {
        try {
            getEnvironment().remove(EnvironmentContext.SYSTEM_ENVIRONMENT);
            EnvironmentContext.getEnvironment();
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("Environment variable 'SYSTEM_ENVIRONMENT' was not set");
        }
    }

    @Test
    public void testGetEnvironment() throws Exception {
        final String serverType = "qa";
        getEnvironment().put(EnvironmentContext.SYSTEM_ENVIRONMENT, serverType);
        assertThat(EnvironmentContext.getEnvironment()).isEqualTo(serverType);
    }

    @Test
    public void testEnvironments() throws Exception {
        // set environment to qa
        setEnvironmentToQa();

        final Config strCfg = ConfigFactory.parseString("all.var.blah:all,qa.var.blah:qa");

        final Config config = EnvironmentContext.resolveEnvConfig(strCfg);

        assertThat(config.getString("var.blah")).isEqualTo("qa");

    }

    @Test
    public void testEnvironmentsVar() throws Exception {
        // set environment to qa
        setEnvironmentToQa();

        final Config strCfg = ConfigFactory.parseString("" +
                "all.var.blah:all," +
                "qa.var.blah:qa," +
                "all.var.ref:${var.blah}");
        // make a reference to something that won't resolve until the end

        final Config config = EnvironmentContext.resolveEnvConfig(strCfg);

        assertThat(config.getString("var.ref")).isEqualTo("qa");

    }

    public static void setEnvironmentToQa() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        getEnvironment().put(EnvironmentContext.SYSTEM_ENVIRONMENT, "qa");
    }

    public static Map<String, String> env = null;
    /**
     * Allow access to cached system environment. Not thread safe.
     *
     * @return cached system environment
     */
    public static Map<String, String> getEnvironment() throws ClassNotFoundException, NoSuchFieldException,
            IllegalAccessException {
        if (env == null) {
            // this is just for testing and may break at anytime
            // protected class
            final Field setter = Class.forName("java.lang.ProcessEnvironment")
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            // private static field
            setter.setAccessible(true);
            env = (Map<String, String>) setter.get(null);
        }
        return env;
    }

}