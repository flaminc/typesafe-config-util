package com.github.flaminc.config;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Flaming 1/19/2015
 */
public interface ReferenceHandler {
    /**
     * Dereference key in some way specific to handler
     *
     * @param oriKey    Original reference key
     * @param keyStr    Original key less first key in rendered form
     * @param config    Config where ref was requested
     * @param fieldType Type requested or null if unknown
     * @return Return object at reference
     */
    public <E> E resolve(@NotNull String oriKey,
                         @NotNull String keyStr,
                         @NotNull Config config,
                         @Nullable Class<E> fieldType);

    /**
     * Clean caches if handler has them.
     */
    public void clean();
}
