package com.sc.cache.exception;

public class CacheLoadException extends RuntimeException{

    public CacheLoadException(String message, RuntimeException ex)
    {
        super(message, ex);
    }
}
