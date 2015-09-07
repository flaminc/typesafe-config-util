package com.github.flaminc.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chris Flaming 1/13/2015
 */
public class EnvironmentContext {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentContext.class);

    final static ConfigResolveOptions ALLOW_UNRESOLVED = ConfigResolveOptions.defaults().setAllowUnresolved(true);
    final static ConfigResolveOptions NO_UNRESOLVED = ConfigResolveOptions.defaults();

    /**
     * environment variable to read the server type from
     *
     * TODO make this configurable
     */
    public static final String SYSTEM_ENVIRONMENT = "SYSTEM_ENVIRONMENT";

    private EnvironmentContext() {

    }

    public static Config resolveEnvConfig() {
        // read the configuration from the default file
        ConfigParseOptions parseOptions = ensureClassLoader(ConfigParseOptions.defaults());
        ClassLoader loader = parseOptions.getClassLoader();
        final Config appConfig = ConfigFactory.load(loader, parseOptions, EnvironmentContext.ALLOW_UNRESOLVED);
        return resolveEnvConfig(appConfig);
    }

    protected static Config resolveEnvConfig(Config config) {
        final Config allConfig = config.getConfig("all");
        final Config envConfig = config.getConfig(getEnvironment());
        final Config combined = envConfig.withFallback(allConfig);
        return combined.resolve(NO_UNRESOLVED);
    }

    /**
     * Determine what environment application is running in. Should return prd, qa, or dev.
     *
     * @return A value form the following: dev,qa,prd (or whatever system admins decide)
     */
    public static String getEnvironment() {

        String env = System.getenv(SYSTEM_ENVIRONMENT);
        if (env == null || "".equals(env.trim())) {
            env = System.getProperty(SYSTEM_ENVIRONMENT);
        }
        if (env == null || "".equals(env.trim())) {
            log.error("No environment settings found for {}", SYSTEM_ENVIRONMENT);
            throw new RuntimeException("Environment variable '" + SYSTEM_ENVIRONMENT + "' was not set");
        }
        return env.trim().toLowerCase();
    }

    private static ConfigParseOptions ensureClassLoader(ConfigParseOptions options) {
        return options.getClassLoader() == null
                ? options.setClassLoader(Thread.currentThread().getContextClassLoader())
                : options;
    }

}
