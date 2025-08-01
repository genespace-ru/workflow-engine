package ru.biosoft.util;

import ru.biosoft.exception.ExceptionRegistry;

public class ObjectExtensionRegistry<T> extends ExtensionRegistrySupport<T>
{

    private Class<T> baseClass;

    public ObjectExtensionRegistry(String extensionPointId, String nameAttribute, Class<T> clazz)
    {
        super(extensionPointId, nameAttribute);
        this.baseClass = clazz;
    }

    public ObjectExtensionRegistry(String extensionPointId, Class<T> clazz)
    {
        this(extensionPointId, "class", clazz);
    }

    /**
     * Subclass this method to define additional initialization steps
     */
    protected void postInit()
    {
    }
    
    //TODO: may be pass args
    @Override public void addElement(String name, String className)
    {
        try
        {
            registerElement(name, className, null);
        }
        catch (Exception e)
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override protected T registerElement(String elementName, String className, Object... args) throws Exception
    {
        init();
        Class<? extends T> elementClass = getClass(className, baseClass);

        T obj = null;

        if( args != null && args.length > 0 )
        {
            Class<?> parTypes[] = new Class<?>[args.length];
            for ( int i = 0; i < args.length; i++ )
            {
                parTypes[i] = args[i].getClass();
            }
            obj = elementClass.getDeclaredConstructor(parTypes).newInstance(args);
        }
        else
            obj = elementClass.getDeclaredConstructor().newInstance();

        addElementInternal(elementName, obj);
        return obj;
    }
}
