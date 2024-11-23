package org.maplestar.syrup.utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for applying and checking cooldowns for multiple unique objects.
 *
 * @param <T> the type that should be put on cooldown
 */
public class CooldownProvider<T> {
    private final Map<T, Duration> cooldownTimes = new HashMap<>();
    private final Duration duration;

    private CooldownProvider(Duration duration) {
        this.duration = duration;
    }

    /**
     * Checks whether the provided object is currently on cooldown because a cooldown has previously been applied.
     * Doesn't apply a cooldown.
     *
     * @param t the object to check
     * @return true if this object is still on cooldown
     */
    public boolean isOnCooldown(T t) {
        if (!cooldownTimes.containsKey(t)) return false;

        return cooldownTimes.get(t).toMillis() > System.currentTimeMillis();
    }

    /**
     * Adds or refreshes the cooldown for an object based on the duration provided during initialization of this class.
     *
     * @param t the object
     */
    public void applyCooldown(T t) {
        cooldownTimes.put(t, duration.plusMillis(System.currentTimeMillis()));
    }

    /**
     * Returns a new instance with the provided {@link Duration}.
     *
     * @param duration the cooldown length
     * @return a new instance
     * @param <T> the type of {@link CooldownProvider}
     */
    public static <T> CooldownProvider<T> withDuration(Duration duration) {
        return new CooldownProvider<>(duration);
    }
}
