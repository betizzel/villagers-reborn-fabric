package com.javic.slimpatch.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Predicate;

/** Persistent TOML values shared by the client and server configuration. */
public final class ConfigStore {
    private final CommentedFileConfig file;

    public ConfigStore(String filename) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(filename);
        file = CommentedFileConfig.builder(path).sync().build();
        file.load();
    }

    public <T> Value<T> define(String key, T defaultValue, Function<Object, T> decoder, Function<T, Object> encoder, Predicate<T> valid) {
        synchronized (file) {
            T value = decode(file.get(key), decoder, valid);
            if (value == null) {
                value = defaultValue;
                file.set(key, encoder.apply(value));
            }
            return new Value<>(this, key, value, encoder, valid);
        }
    }

    private static <T> T decode(Object raw, Function<Object, T> decoder, Predicate<T> valid) {
        try {
            T value = decoder.apply(raw);
            return value != null && valid.test(value) ? value : null;
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    public Value<Boolean> bool(String key, boolean value) {
        return define(key, value, raw -> raw instanceof Boolean b ? b : null, v -> v, v -> true);
    }

    public Value<String> string(String key, String value) {
        return define(key, value, raw -> raw instanceof String s ? s : null, v -> v, v -> true);
    }

    public Value<Integer> integer(String key, int value, int min, int max) {
        return define(key, value, raw -> raw instanceof Integer i ? i : raw instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE ? l.intValue() : null,
                v -> v, v -> v >= min && v <= max);
    }

    public Value<Double> decimal(String key, double value, double min, double max) {
        return define(key, value, raw -> raw instanceof Number n ? n.doubleValue() : null,
                v -> v, v -> Double.isFinite(v) && v >= min && v <= max);
    }

    public <E extends Enum<E>> Value<E> enumeration(String key, E value, Class<E> type) {
        return define(key, value, raw -> raw instanceof String s ? Enum.valueOf(type, s) : null, Enum::name, v -> true);
    }

    public void save() {
        synchronized (file) { file.save(); }
    }

    public static final class Value<T> {
        private final ConfigStore store;
        private final String key;
        private final Function<T, Object> encoder;
        private final Predicate<T> valid;
        private volatile T value;

        private Value(ConfigStore store, String key, T value, Function<T, Object> encoder, Predicate<T> valid) {
            this.store = store;
            this.key = key;
            this.value = value;
            this.encoder = encoder;
            this.valid = valid;
        }

        public T get() { return value; }

        public void set(T value) {
            if (value == null || !valid.test(value)) throw new IllegalArgumentException("Invalid value for " + key + ": " + value);
            synchronized (store.file) {
                store.file.set(key, encoder.apply(value));
                this.value = value;
            }
        }
    }
}
