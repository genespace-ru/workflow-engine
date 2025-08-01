package ru.biosoft.server.servlets.webservices.providers;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.exception.InitializationException;

//TODO: new class instead of Registry-based ru.biosoft.server.servlets.webservices.providers.WebProviderFactory
public class WebProviderFactory
{

    private static WebProviderFactory instance = new WebProviderFactory();
    private Map<String, WebProvider> providers;

    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    protected final void init()
    {
        if( !initialized )
        {
            synchronized (this)
            {
                if( !initialized )
                {
                    if( initializing )
                        throw new InitializationException("Concurrent initialization of WebProviderFactory");
                    initializing = true;
                    try
                    {
                        providers = new HashMap<>();
                    }
                    finally
                    {
                        initializing = false;
                        initialized = true;
                    }
                }
            }
        }
    }

    private void registerProviderInternal(String name, WebProvider provider)
    {
        init();
        providers.put(name, provider);
    }

    private WebProvider getProviderInternal(String name)
    {
        init();
        return providers.get(name);
    }

    public static void registerProvider(String name, WebProvider provider)
    {
        instance.registerProviderInternal(name, provider);
    }

    public static WebProvider getProvider(String name)
    {
        return instance.getProviderInternal(name);
    }

}
