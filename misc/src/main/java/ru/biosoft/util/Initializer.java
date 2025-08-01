package ru.biosoft.util;

import ru.biosoft.access.exception.InitializationException;

public abstract class Initializer
{
    private volatile boolean initialized = false;
    private volatile boolean initializing = false;


    protected void init()
    {
        if( !initialized )
        {
            synchronized (this)
            {
                if( !initialized )
                {
                    if( initializing )
                        throw new InitializationException("Concurrent initialization of " + getClass().getName());
                    initializing = true;
                    try
                    {
                        initCommonClasses();
                        initDataElementTypeDrivers();
                        initDataTypes();
                        initProviders();
                        initServices();
                        initTransformers();
                        initBeans();
                        initTemplates();
                        initBeanProviders();
                        initTableResolvers();
                        initPerspectives();
                        initFileTypes();

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


    protected void initTableResolvers()
    {
        //WebTablesProvider.addTableResolver("NAME", "CLASS");

    }

    protected void initCommonClasses()
    {
        //TODO
    }

    protected void initDataElementTypeDrivers()
    {
        //DataElementTypeRegistry.registerDataElementTypeDriver("DRIVER CLASS");
    }

    protected void initDataTypes()
    {
        //DataType.addDataType("DATA TYPE");
    }

    protected void initProviders()
    {
        //WebProviderFactory.registerProvider("NAME", PROVIDER OBJECT);
    }

    protected void initServices()
    {
        //ServiceRegistry.registerService("SERVICE NAME", SERVICE OBJECT);
    }

    protected void initTransformers()
    {
        //TransformerRegistry.addTransformer("NAME", "TRANSFORMER CLASS", "INPUT CLASS", "OUTPUT CLASS");
    }

    protected void initBeans()
    {
        //BeanRegistry.registerBeanProvider("NAME", "BEAN CLASS");
    }

    protected void initTemplates()
    {
        //TemplateRegistry.registerTemplate("TEMPLATE NAME", "ANY CLASS WITH RESOURCE FOLDER INSTEAD OF PLUGIN NAME", "OTHER ARGUMENTS FOR TEMPLATE: FILE PATH, DESCRIPTION, IS BREAF, ORDER");
    }

    protected void initBeanProviders()
    {
        //BeanRegistry.registerBeanProvider("PREFIX", "BEAN PROVIDER CLASS");
    }

    protected void initFileTypes()
    {
        // FileTypeRegistry.register(String name, String[] extensions, String transformerClassName, FileTypePriority priority, String description);
    }

    protected void initPerspectives()
    {
        //PerspectiveRegistry.registerPerspective("NAME", "CLASS", args);
    }
}
