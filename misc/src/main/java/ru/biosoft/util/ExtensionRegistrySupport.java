package ru.biosoft.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.exception.InitializationException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.templates.TemplateInfo;

/**
 * Class refactored for slim version
 * 
 * @author anna
 *
 */
public abstract class ExtensionRegistrySupport<T> implements Iterable<T>
{
    private static final Logger log = Logger.getLogger(ExtensionRegistrySupport.class.getName());
    private final String extensionPointId;
    private final String nameAttribute;
    protected volatile List<T> extensions;
    protected volatile Map<String, T> nameToExtension;
    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    //TODO: constructors not needed anymore, refactor parameters
    /**
     * @param extensionPointId - id of extension point like "ru.biosoft.access.import"
     * @param nameAttribute - required attribute which identifies the extension (particularly will be used for error messages and duplicates check)
     */
    public ExtensionRegistrySupport(String extensionPointId, String nameAttribute)
    {
        this.extensionPointId = extensionPointId;
        this.nameAttribute = nameAttribute;
    }

    public ExtensionRegistrySupport(String extensionPointId)
    {
        this(extensionPointId, "name");
    }

    /**
     * @return collection of registered extensions
     */
    protected List<T> doGetExtensions()
    {
        init();
        return extensions;
    }

    /**
     * Returns extension by given name
     * @param name
     * @return extension or null if no such extension exists
     */
    public T getExtension(String name)
    {
        if( name == null )
            return null;
        init();
        return nameToExtension.get(name);
    }

    @Override
    public Iterator<T> iterator()
    {
        init();
        return Collections.unmodifiableList(extensions).iterator();
    }

    protected final void init()
    {
        if(!initialized)
        {
            synchronized(this)
            {
                if(!initialized)
                {
                    if(initializing)
                        throw new InitializationException("Concurrent initialization of extension point " + extensionPointId);
                    initializing = true;
                    try
                    {
                        nameToExtension = new HashMap<>();
                        extensions = new ArrayList<>();
                        postInit();
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

    /**
     * Subclass this method to define additional initialization steps
     */
    protected void postInit()
    {
    }

    public StreamEx<T> stream()
    {
        init();
        return StreamEx.of( extensions );
    }

    public StreamEx<String> names()
    {
        init();
        return StreamEx.ofKeys( nameToExtension );
    }

    public EntryStream<String, T> entries()
    {
        init();
        return EntryStream.of( nameToExtension );
    }

    protected void addElementInternal(String name, T element)
    {
        extensions.add(element);
        nameToExtension.put(name, element);
    }

    protected abstract T registerElement(String elementName, String className, Object... args) throws Exception;

    public abstract void addElement(String elementName, String className);

    //TODO: may be not needed
    protected static <K> Class<? extends K> getClass(String className)
    {
        Class<? extends K> clazz;
        try
        {
            clazz = (Class<? extends K>) ClassLoading.loadClass(className);
        }
        catch (Exception e)
        {
            throw ExceptionRegistry.translateException(e);
        }
        return clazz;
    }

    protected static <K> Class<? extends K> getClass(String className, @Nonnull Class<K> parentClass)
    {
        Class<? extends K> clazz;
        try
        {
            clazz = ClassLoading.loadSubClass(className, parentClass);
        }
        catch (Exception e)
        {
            throw ExceptionRegistry.translateException(e);
        }
        return clazz;
    }

}
