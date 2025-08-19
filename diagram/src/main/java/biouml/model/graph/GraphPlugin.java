package biouml.model.graph;

import java.lang.reflect.Field;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.ExtensionRegistrySupport;

public class GraphPlugin extends ExtensionRegistrySupport<LayouterDescriptor>
{
    private static final GraphPlugin instance = new GraphPlugin();
    
    private GraphPlugin()
    {
        super("ru.biosoft.plugins.graph.layout", LayouterDescriptor.TITLE_ATTR);
    }

    public static List<LayouterDescriptor> loadLayouters()
    {
        return instance.stream().toList();
    }
    
    public static LayouterDescriptor getLayouter(String name)
    {
        return instance.getExtension( name );
    }


    @Override
    protected LayouterDescriptor registerElement(String elementName, String className, Object... args) throws Exception
    {
        init();
        String description = (String) args[0];
        boolean isPublic = (boolean) args[1];
        Class clazz = getClass( className );
        LayouterDescriptor ld = new LayouterDescriptor( clazz, elementName, description, isPublic );
        DynamicPropertySet properties = ld.getDefaultParameters();
        if( args.length > 2 )
        {
            for ( int i = 0; i < (args.length - 2) % 2; i++ )
            {
                String propName = (String) args[2 + i * 2];
                Object value = args[2 + i * 2 + 1];
                Field field;
                try
                {
                    field = clazz.getDeclaredField( propName );
                    Class propertyType = field.getType();
                    DynamicProperty dp = new DynamicProperty( propName, propertyType, value );
                    properties.add( dp );
                }
                catch (Exception e)
                {
                }
            }
        }
        addElementInternal( elementName, ld );
        return ld;
    }

    @Override
    public void addElement(String elementName, String className)
    {
        // TODO Auto-generated method stub

    }
}
