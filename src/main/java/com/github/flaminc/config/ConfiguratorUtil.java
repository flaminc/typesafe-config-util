package com.github.flaminc.config;

import com.typesafe.config.*;
import com.typesafe.config.impl.ConfigBridge;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Chris Flaming 1/8/2015
 */
public class ConfiguratorUtil {
    private static final Logger log = LoggerFactory.getLogger(ConfiguratorUtil.class);

    private static final ValueTypeException valueTypeError = new ValueTypeException();

    @NotNull
    public static ConfiguratorUtil noConfig() {
        return new ConfiguratorUtil();
    }

    @NotNull
    public static ConfiguratorUtil withConfig(Config rootConfig) {
        return new ConfiguratorUtil(rootConfig);
    }

    @NotNull
    public static ConfiguratorUtil withConfig(Config rootConfig, Map<String, ReferenceHandler> refHandlers) {
        final ConfiguratorUtil configuratorUtil = new ConfiguratorUtil(rootConfig);
        configuratorUtil.refHandler.putAll(refHandlers);
        return configuratorUtil;
    }

    private final Map<String, Object> references;

    private final Config root;

    private final Map<String, ReferenceHandler> refHandler = new HashMap<String, ReferenceHandler>();

    private ConfiguratorUtil() {
        references = new HashMap<String, Object>();
        root = ConfigFactory.empty();
    }

    private ConfiguratorUtil(Config rootConfig) {
        references = new HashMap<String, Object>();
        root = rootConfig;
    }

    /**
     * Constructor from root config
     *
     * @param fieldType Type of object to call constructor on. If null, then config is consulted with '~type' key.
     * @return Newly constructed
     * @see ConfiguratorUtil#construct(com.typesafe.config.Config, Class)
     */
    public <E> E construct(@Nullable Class<E> fieldType) {
        return construct(null, fieldType);
    }

    /**
     * Uses the constructor of the specified fieldType and data at config to construct a new class. If a '~ref' field
     * is provided, then the object will use the singleton at the provided reference. The '~constructor'
     * field is mandatory (if not '~ref' field) and values consists of a list of constructor values. The '~type' key will
     * be used if the fieldType is set to null; otherwise, it will be ignored.
     *
     * @param config    Configuration to use to look up keys. Otherwise root config is used.
     * @param fieldType Type of object to call constructor on. If null, then config is consulted with '~type' key.
     * @return Newly constructed class
     */
    public <E> E construct(@Nullable Config config, @Nullable Class<E> fieldType) {
        if (config == null) {
            config = root;
        }
        // is this a reference?
        if (config.hasPath("~ref")) {
            return resolveReference(config, fieldType);
        }

        final List<ConfigValue> args;
        // either constructor route or bean route
        final boolean constructorRoute;
        if (config.hasPath("~constructor")) {
            args = config.getList("~constructor");
            constructorRoute = true;
        } else {
            args = Collections.emptyList();
            constructorRoute = false;
        }

        if (fieldType == null || hasType(config)) {
            // look up field type from config
            fieldType = getType(config);
        }
        Object[] callArgs = new Object[args.size()];
        Class<?>[] argTypes = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            final ConfigValue arg = args.get(i);
            try {
                callArgs[i] = inferObject(arg);
            } catch (ValueTypeException e) {
                throw new RuntimeException("Cannot convert constructor position " + i + " with value arg");
            }
            argTypes[i] = callArgs[i].getClass();
        }
        try {
            final E instance;

            if (!constructorRoute && fieldType == Map.class) {
                // handle map
                Map<Object, Object> map = createMap();

                final Set<Map.Entry<String, ConfigValue>> entries = ConfigBridge.getEntries(config);
                for (Map.Entry<String, ConfigValue> entry : entries) {
                    final ConfigValue value = entry.getValue();
                    try {
                        map.put(entry.getKey(), inferObject(value));
                    } catch (ValueTypeException e) {
                        throw new RuntimeException("Cannot convert value '" + value + "' for key '" +
                                entry.getKey() + "'");
                    }
                }

                instance = (E) map;
            } else {
                final Constructor<E> constructor = ConstructorUtils.getMatchingAccessibleConstructor(fieldType, argTypes);
                if (constructor == null) {
                    throw new IllegalArgumentException("Cannot find constructor of types: " + Arrays.toString(argTypes) +
                            " for " + fieldType);
                }
                instance = constructor.newInstance(callArgs);
            }
            if (!constructorRoute) {
                // load via the bean route
                loadClass(config, instance);
            }
            return instance;
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <E> Class<E> getType(Config config) {
        Class<E> fieldType;
        final String type = config.getString("~type");
        try {
            fieldType = (Class<E>) Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        return fieldType;
    }

    private boolean hasType(Config config) {
        return config.hasPath("~type");
    }

    private <E> E resolveReference(@NotNull Config config, @Nullable Class<E> fieldType) {
        final String refKey = config.getString("~ref");
        Pair<String, String> split = ConfigBridge.pathToFirstRest(refKey);

        if (fieldType == null && hasType(config)) {
            fieldType = getType(config);
        }
        final String handlerKey = split.getLeft();
        if ("var".equals(handlerKey)) {
            // look it up as its in the default namespace ('var')
            if (references.containsKey(refKey)) {
                return (E) references.get(refKey);
            } else {
                final E obj = getObject(root, refKey, fieldType);
                references.put(refKey, obj);
                return obj;
            }
        } else {
            // pass the keys to the handler registered for it
            final ReferenceHandler handler = refHandler.get(handlerKey);
            if (handler == null) {
                throw new RuntimeException("Cannot find reference handler named: '" + handlerKey + "'");
            }

            final E resolve = handler.resolve(refKey, split.getRight(), config, fieldType);
            if (fieldType.isPrimitive() && resolve == null) {
                throw new RuntimeException(String.format("Primitive type %s cannot resolve to null from handler " +
                                "for reference key %s sent to '%s' handler with type %s",
                        fieldType, refKey, handlerKey, handler.getClass().getName()));
            }
            return resolve;
        }
    }

    public <E> E constructFromList(@NotNull ConfigList list, Class<E> fieldType) {

        if (fieldType == Map.class) {
            // handle map
            Map<Object, Object> map = createMap();

            if (list.size() % 2 != 0) {
                throw new RuntimeException("Map must have list with even number of entries");
            }
            for (int i = 0; i < list.size(); i += 2) {
                Object key;
                Object value;
                try {
                    key = inferObject(list.get(i));
                } catch (ValueTypeException e) {
                    throw new RuntimeException("Cannot convert value at list position " + (i) + " with value '" +
                            list.get(i) + "'");
                }
                try {
                    value = inferObject(list.get(i + 1));
                } catch (ValueTypeException e) {
                    throw new RuntimeException("Cannot convert value at list position " + (i + 1) + " with value '" +
                            list.get(i + 1) + "'");
                }
                map.put(key, value);
            }

            return (E) map;
        }
        throw new RuntimeException("List of type " + fieldType.getSimpleName() + " not yet implemented");
    }

    public Object inferObject(@NotNull ConfigValue value) throws ValueTypeException {
        final ConfigValueType valueType = value.valueType();

        switch (valueType) {
            case OBJECT:
                final Config config = ConfigBridge.configFromObjectValueType(value);
                return construct(config, null);
            case STRING:
                return value.unwrapped();
            case NUMBER:
                return value.unwrapped();
            case BOOLEAN:
                return value.unwrapped();
            default:
                throw valueTypeError;
        }
    }

    /**
     * Grab an object from the path using the config. FieldType will be the type of object that will be grabbed. If
     * object contained in the config at the path is a simple type (not a config map), then it is converted to the
     * required type. Such as a {@link java.lang.String} is expected, then the a string is pulled from the config. If
     * the config value is a config map, then reflection will occur such as using '~constructor' key syntax described in
     * {@link ConfiguratorUtil#construct(com.typesafe.config.Config, Class)}.
     *
     * @param config    Configuration to use to look up object in.
     * @param path      Path to look up object at.
     * @param fieldType Type of class expected from object.
     * @return object requested or {@code null}
     */
    public <E> E getObject(@NotNull Config config, @NotNull String path, @Nullable Class<E> fieldType) {
        if (config.hasPath(path)) {

            if (fieldType == null) {
                // use inference to determine
                try {
                    return (E) inferObject(config.getValue(path));
                } catch (ValueTypeException e) {
                    throw new RuntimeException("Cannot resolve ~type of reference at " + path);
                }
            } else {

                ConfigValue value = config.getValue(path);
                final ConfigValueType valueType = value.valueType();

                switch (valueType) {
                    case OBJECT:
                        final Config toConstruct = config.getConfig(path);
                        return construct(toConstruct, fieldType);
                    case LIST:
                        return constructFromList(config.getList(path), fieldType);
                }

                if (fieldType == String.class) {
                    return (E) config.getString(path);
                } else if (fieldType.isEnum()) {
                    return (E) Enum.valueOf((Class<Enum>) fieldType, config.getString(path));
                } else if (fieldType == Integer.class || fieldType == int.class) {
                    return (E) Integer.valueOf(config.getInt(path));
                } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                    return (E) Boolean.valueOf(config.getBoolean(path));
                } else if (fieldType == Double.class || fieldType == double.class) {
                    return (E) Double.valueOf(config.getDouble(path));
                }

            }
        } else if (!fieldType.isPrimitive()) {
            // if the fieldType isn't primitive then a missing path will be interpreted as a null value
            return null;
        }
        throw new IllegalArgumentException("Not implemented for path: " + path + " of type: " + fieldType.getName());
    }

    /**
     * Load class from root config
     *
     * @param instance Instance to load using config file
     * @param <E>      Generic instance type.
     * @return Instance passed in after loading for chaining calls.
     * @see ConfiguratorUtil#loadClass(com.typesafe.config.Config, Object)
     */
    public <E> E loadClass(@NotNull E instance) {
        return loadClass(null, instance);
    }

    /**
     * Populate passed in class using the config provided. Fields that will be wired must be tagged with
     * {@link javax.annotation.Resource} annotation. Method to be call after setting properties must be annotated with
     * {@link javax.annotation.PostConstruct}.
     * See {@link ConfiguratorUtil#construct(com.typesafe.config.Config, Class)} for more syntax
     * details.
     *
     * @param config   Config map to set class with, If null then root is used.
     * @param instance Instance to load using config file
     * @param <E>      Generic instance type.
     * @return Instance passed in after loading for chaining calls
     * @see ConfiguratorUtil#construct(com.typesafe.config.Config, Class)
     */
    public <E> E loadClass(@Nullable Config config, @NotNull E instance) {
        // hopefully this works when class is inherited
        final Field[] fields = instance.getClass().getDeclaredFields();
        if (config == null) {
            config = root;
        }
        if (config != null && !config.isEmpty()) {
            for (Field field : fields) {
                // find fields that need wired
                if (field.getAnnotation(Resource.class) != null) {
                    try {
                        final Class<?> fieldType = field.getType();
                        final String path = field.getName();
                        if (config.hasPath(path)) {
                            final Object value = getObject(config, path, fieldType);
                            log.trace("wiring in field {} with {}", path, value);
                            field.setAccessible(true);
                            field.set(instance, value);
                        }
                    } catch (RuntimeException e) {
                        log.error("failed to wire field", e);
                        throw e;
                    } catch (IllegalAccessException e) {
                        log.error("failed to wire field", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            log.warn("Config passed to loadConfig was empty");
        }
        // determine if we need to call the post constructor/init
        final Method[] methods = instance.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    method.invoke(instance);
                } catch (Exception e) {
                    log.error("Cannot call PostConstruct", e);
                    throw new RuntimeException("Cannot call PostConstruct", e);
                }
            }
        }
        return instance;
    }

    public void addHandler(String key, ReferenceHandler handler) {
        refHandler.put(key, handler);
    }

    public void clean() {
        for (ReferenceHandler handler : refHandler.values()) {
            handler.clean();
        }
    }

    /**
     * Determines type of maps that are created by app
     *
     * @return Map instance
     */
    protected
    @NotNull
    HashMap<Object, Object> createMap() {
        return new HashMap<Object, Object>();
    }

    private static class ValueTypeException extends Exception {
    }
}
