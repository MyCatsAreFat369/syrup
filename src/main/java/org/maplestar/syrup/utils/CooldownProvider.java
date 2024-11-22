package org.maplestar.syrup.utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CooldownProvider<T> {
    private final Map<T, Duration> cooldownTimes = new HashMap<>();
    private final Duration duration;

    private CooldownProvider(Duration duration)
    {
        this.duration = duration;
    }

    public boolean isOnCooldown(T t) {
        if (!cooldownTimes.containsKey(t)) return false;

        return cooldownTimes.get(t).toMillis() > System.currentTimeMillis();
    }

    public void applyCooldown(T t) {
        cooldownTimes.put(t, duration.plusMillis(System.currentTimeMillis()));
    }

    public static <T> CooldownProvider<T> withDuration(Duration duration) {
        return new CooldownProvider<>(duration);
    }
}
