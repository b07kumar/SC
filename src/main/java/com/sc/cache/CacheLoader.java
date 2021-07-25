package com.sc.cache;

import java.util.Optional;

@FunctionalInterface
public interface CacheLoader<K, V> {
    public V load(K key);
}
