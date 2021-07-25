package com.sc.cache.impl;

import com.sc.cache.Cache;
import com.sc.cache.exception.CacheLoadException;
import com.sc.cache.CacheLoader;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadThroughCacheImpl<K, V> implements Cache<K, V>{

    private volatile Map<K, Optional<V>> cache = new HashMap<>();
    private CacheLoader<K, V> loader;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Logger logger = Logger.getLogger(ReadThroughCacheImpl.class);

    public ReadThroughCacheImpl(CacheLoader<K, V> loader)
    {
        this.loader = loader;
    }

    @Override
    public V get(K key) {
        Optional<V> value = null;
        if( key != null ) {
            value = computeIfAbsent(key);
        }
        else
            throw new NullPointerException("Null key not permitted in the cache");
        return value.isEmpty()?null:value.get();
    }

    private Optional<V> computeIfAbsent(K key)
    {
        Optional<V> value = readFromCache(key);
        if( value == null )
            value = compute(key);
        return value;
    }

    private Optional<V> readFromCache(K key)
    {
        Optional<V> value = null;
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        try{
            value = cache.get(key);
        }finally {
            readLock.unlock();
        }
        return value;
    }

    private Optional<V> compute(K key)
    {
        logger.info("Loading data for key: %s".formatted(key));
        Optional<V> value = null;
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            Optional<V> cached = readFromCache(key);

            if( cached == null ) {
                value = Optional.ofNullable(loader.load(key));
                cache.put(key, value);
            }
            else
                return cached;
        }
        catch (RuntimeException ex)
        {
            logger.error("Error while loading cache for key: %s".formatted(key));
            throw new CacheLoadException("Error while loading cache for key:%s".formatted(key), ex);
        }
        finally {
            writeLock.unlock();
        }
        return value;
    }
}