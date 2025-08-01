package ru.biosoft.server;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.exception.InitializationException;

/**
 * Service registry
 */
//TODO: new class instead of Registry-based ru.biosoft.server.ServiceRegistry
public class ServiceRegistry
{
    private static ServiceRegistry instance = new ServiceRegistry();
    private Map<String, Service> services;

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
                        throw new InitializationException("Concurrent initialization of ServiceRegistry");
                    initializing = true;
                    try
                    {
                        services = new HashMap<>();
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

    private void addService(String name, Service service)
    {
        init();
        services.put(name, service);
    }

    private Service getServiceInternal(String name)
    {
        if( name == null )
            return null;
        init();
        return services.get(name);
    }

    /**
     * Register new service
     * 
     * @param name
     * @param service 
     * Should be called manually from some initializer class
     */
    public static void registerService(String name, Service service)
    {
        instance.addService(name, service);
    }

    /**
     * Return service by name
     */
    public static Service getService(String name)
    {
        if( name == null )
            return null;
        return instance.getServiceInternal(name);
    }
}
