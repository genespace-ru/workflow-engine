package ru.biosoft.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TextUtil2;


/**
 * Facade for templates operations.
 */
public class TemplateRegistry extends ExtensionRegistrySupport<TemplateInfo>
{
    public static final String FILTER_ELEMENT = "filter";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String NAME_ATTR = "name";
    public static final String FILE_ATTR = "file";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String ISBRIEF_ATTR = "isBrief";
    public static final String ORDER_ATTR = "order";
    public static final String CLASS_ATTR = "class";
    public static final String SUBCLASSES_ATTR = "subclasses";
    public static final String METHOD_ATTR = "method";
    public static final String JAVASCRIPT_ATTR = "javascripts";
    public static final String VALUE_ATTR = "value";
    public static final String ISREGEXP_ATTR = "isRegexp";

    private static Logger log = Logger.getLogger(TemplateRegistry.class.getName());
    private static final TemplateRegistry instance = new TemplateRegistry();
    
    private static final ExtensionRegistrySupport<Object> contextItems = new ObjectExtensionRegistry<Object>("ru.biosoft.templates.contextItem", NAME_ATTR, Object.class);
    
    static
    {
        try
        {
            Properties props = new Properties();
            props.setProperty("velocimacro.context.localscope", "true");
        
            props.setProperty("resource.loader", "class");
            props.setProperty("class.resource.loader.class", "ru.biosoft.templates.ClasspathResourceLoader");
            props.setProperty("class.resource.loader.cache", "false");
        
            //TODO: maven requires other resources folder
            //props.setProperty("velocimacro.library", "resources/displayMacros.vm, resources/processMacros.vm");
            props.setProperty("velocimacro.library", "ru/biosoft/templates/displayMacros.vm, ru/biosoft/templates/processMacros.vm");

            props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");

            // experimental, possble fix for 
            // Runtime : ran out of parsers. Creating a new one.  Please increment the parser.pool.size property. The current value is too small.
            props.setProperty( "parser.pool.size", "50" );
        
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            synchronized( TemplateRegistry.class )
            {
                try
                {
                    Velocity.init(props);
                }
                catch (Exception e)
                {
                    log.log(Level.SEVERE, e.getMessage());
                }
            }
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to initialize Velocity engine", e);
        }
    }
    
    public static void initialize()
    {
        // Just to perform static initialization
    }
    
    private TemplateRegistry()
    {
        super("ru.biosoft.templates.template", NAME_ATTR);
    }
    
    /**
     * Returns template info objects that are suitable
     * for the specified object.
     */
    public static @Nonnull TemplateInfo[] getSuitableTemplates(Object obj)
    {
        return instance.stream().filter( info -> info.isSuitable( obj ) ).sortedByInt( TemplateInfo::getOrder )
                .toArray( TemplateInfo[]::new );
    }

    /**
     * Apply template to data element
     */
    public static @Nonnull StringBuffer mergeTemplate(Object de, String templateName)
    {
        try
        {
            TemplateInfo templateInfo = instance.getExtension(templateName);

            if( templateInfo.isSuitable(de) )
            {
                Template template = templateInfo.getTemplate();
                return mergeTemplate(de, template);
            }
            return new StringBuffer("Template '"+templateInfo.getName()+"' is not suitable for "+de);
        }
        catch( Throwable t )
        {
            BiosoftVelocityException ex = new BiosoftVelocityException(t, templateName, de);
            ex.log();
            String[] message = TextUtil2.split(ex.getMessage(), '\n');
            StringBuffer result = new StringBuffer();
            result.append("<div class='log_error'>").append(message[0]).append("</div>");
            for(int i=1; i<message.length; i++)
            {
                result.append("<div class='log_warning'>").append(message[i]).append("</div>");
            }
            return result;
        }
    }

    /**
     * Apply template to data element
     */
    public static @Nonnull StringBuffer mergeTemplate(Object de, Template template) throws Exception, IOException
    {
        VelocityContext context = new VelocityContext();
        
        contextItems.entries().prepend( "de", de ).forKeyValue( context::put );

        StringWriter sw = new StringWriter();
        template.merge( context, sw );
        return sw.getBuffer();
    }

    @Override protected TemplateInfo registerElement(String elementName, String className, Object... args) throws Exception
    {
        init();
        String filePath = (String) args[0];
        String description = (String) args[1];
        boolean isBrief = (boolean) args[2];
        int order = (int) args[3];
        TemplateFilter filter = null;
        if( args.length > 4 )
        {
            filter = (TemplateFilter) args[4];
        }
        Class clazz = getClass(className);
        TemplateInfo result = new TemplateInfo(elementName, description, isBrief, clazz, filePath, filter, order);
        addElementInternal(elementName, result);
        return result;
    }

    public static void registerTemplate(String elementName, String className, Object... args)
    {
        try
        {
            instance.registerElement(elementName, className, args);
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Template " + elementName + " was not registered.", e);
        }
    }

    @Override public void addElement(String elementName, String className)
    {
        throw new UnsupportedOperationException("Can not add template with this method");

    }

    @Override protected void postInit()
    {
        addContextItem( "utils", "ru.biosoft.templates.Formatter" );
    }

    public static void addContextItem(String name, String className)
    {
        contextItems.addElement( name, className );
    }
}

