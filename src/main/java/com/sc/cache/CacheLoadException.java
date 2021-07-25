package com.sc.cache;

public class CacheLoadException extends RuntimeException{

    public CacheLoadException(String message, RuntimeException ex)
    {
        super(message, ex);
    }
}
