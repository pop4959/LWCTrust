package org.popcraft.lwctrust;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extremely basic cache implementation using a LinkedHashMap with a maximum cache size.
 */
public class Cache<K, V> extends LinkedHashMap<K, V> {

    private int max;

    public Cache(int max) {
        this.max = max;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return this.size() > this.max;
    }

}
