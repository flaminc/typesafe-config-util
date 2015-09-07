package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

/**
 * @author Chris Flaming 1/15/2015
 */
public class ConfigBridge {

    private ConfigBridge() {

    }

    public static Config configFromObjectValueType(ConfigValue value) {
        return ((AbstractConfigObject) value).toConfig();
    }

    public static Set<Map.Entry<String, ConfigValue>> getEntries(Config config) {
        // did not like how config.entrySet() works where keys are paths
        return config.root().entrySet();
    }

    public static Pair<String, String> pathToFirstRest(String pathStr) {
        Path path = Path.newPath(pathStr);
        if (path.first() == null || path.remainder() == null) {
            throw new RuntimeException("Expected path to have at least 2 parts in [" + pathStr + "]");
        }
        return Pair.of(path.first(), path.remainder().render());
    }
}
