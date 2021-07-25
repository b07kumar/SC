package com.sc.cache.impl;

import com.sc.cache.Cache;
import com.sc.cache.CacheLoadException;
import com.sc.cache.CacheLoader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ReadthroughCacheTest {

    @Test
    void testGet() {
        Cache<String, String> cache = new ReadThroughCacheImpl<>( (k) -> "test");
        assertEquals( "test", cache.get("test"));
    }

    @Test
    void testGetNullKey() {
        Cache<String, String> cache = new ReadThroughCacheImpl<>( (k) -> "test");
        assertThrows( NullPointerException.class, () -> cache.get(null));
    }

    @Test
    void testLoadCalledOnlyOnce() {
        CacheLoader<String, String> loader = spy(new TestCacheLoader());
        Cache<String, String> cache = new ReadThroughCacheImpl<>( loader );
        cache.get("test1");
        cache.get("test1");
        verify(loader, times(1)).load(anyString());
    }

    @Test
    void testLoadCalledOnlyOnceWhenNullFromLoad() {
        CacheLoader<String, String> loader = spy(new TestCacheLoader());
        Cache<String, String> cache = new ReadThroughCacheImpl<>( loader );
        cache.get("test6");
        cache.get("test6");
        verify(loader, times(1)).load(anyString());
    }

    @Test
    void testLoadCalledOnlyOnceMultithread() throws InterruptedException {
        CacheLoader<String, String> loader = spy(new TestMuthiThreadedCacheLoader());
        Cache<String, String> cache = new ReadThroughCacheImpl<>( loader );
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Collection taskList = Arrays.asList((Callable)()-> cache.get("test1"),
                (Callable)() -> cache.get("test1"),
                (Callable)() -> cache.get("test1"),
                (Callable)() -> cache.get("test1"),
                (Callable)() -> cache.get("test1"));
        executor.invokeAll(taskList);
        executor.shutdown();
        verify(loader, times(1)).load(anyString());
    }

    private class TestMuthiThreadedCacheLoader implements CacheLoader<String, String>
    {
        @Override
        public String load(String key) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "test";
        }
    }

    private class TestCacheLoader implements CacheLoader<String, String>
    {
        private Map<String, String> loaderMap = Map.of("test1","test1",
                "test2","test2",
                "test3","test3",
                "test4","test4",
                "test5","test5");
        @Override
        public String load(String key) {
            return loaderMap.get(key);
        }
    }

    @Test
    void testLoadRuntimeException() {
        CacheLoader<String, String> loader = new TestRunTimeExceptionCacheLoader();
        Cache<String, String> cache = new ReadThroughCacheImpl<>( loader );
        assertThrows(CacheLoadException.class, () -> cache.get("test6"));
    }

    private class TestRunTimeExceptionCacheLoader implements CacheLoader<String, String>
    {
        private Map<String, String> loaderMap;
        @Override
        public String load(String key) {
            return loaderMap.get(key);
        }
    }
}
